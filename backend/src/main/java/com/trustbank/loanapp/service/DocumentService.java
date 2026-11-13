package com.trustbank.loanapp.service;

import com.trustbank.loanapp.common.ApiException;
import com.trustbank.loanapp.domain.DocumentType;
import com.trustbank.loanapp.domain.LoanApplication;
import com.trustbank.loanapp.domain.LoanDocument;
import com.trustbank.loanapp.domain.LoanStatus;
import com.trustbank.loanapp.repo.LoanApplicationRepository;
import com.trustbank.loanapp.repo.LoanDocumentRepository;
import com.trustbank.loanapp.security.AuthUser;
import com.trustbank.loanapp.storage.DocumentStorage;
import com.trustbank.loanapp.web.dto.DtoMapper;
import com.trustbank.loanapp.web.dto.LoanDtos.DocumentDto;
import java.io.IOException;
import java.io.InputStream;
import java.time.Clock;
import java.util.Arrays;
import java.util.Set;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

@Service
public class DocumentService {
    private static final Logger log = LoggerFactory.getLogger(DocumentService.class);
    public static final long MAX_FILE_BYTES = 5_242_880;
    private static final Set<LoanStatus> OPEN = Set.of(LoanStatus.SUBMITTED,
            LoanStatus.PENDING_DOCUMENTS, LoanStatus.AWAITING_ADDITIONAL_INFO);
    private final LoanApplicationRepository applications;
    private final LoanDocumentRepository documents;
    private final LoanStateMachine stateMachine;
    private final DocumentStorage storage;
    private final Clock clock;

    public DocumentService(LoanApplicationRepository applications, LoanDocumentRepository documents,
            LoanStateMachine stateMachine, DocumentStorage storage, Clock clock) {
        this.applications = applications;
        this.documents = documents;
        this.stateMachine = stateMachine;
        this.storage = storage;
        this.clock = clock;
    }

    @Transactional
    public DocumentDto upload(UUID borrowerId, UUID applicationId, DocumentType type, MultipartFile file) {
        LoanApplication application = applications.findById(applicationId)
                .filter(a -> a.getApplicantId().equals(borrowerId))
                .orElseThrow(() -> ApiException.notFound("Application"));
        if (!OPEN.contains(application.getStatus()))
            throw ApiException.rule("DOCUMENTS_CLOSED", "Documents can no longer be added to this application");
        if (type == null) throw ApiException.badRequest("DOCUMENT_TYPE_REQUIRED", "Choose a document type");
        validate(file);
        String name = safeFilename(file.getOriginalFilename());
        String fileId;
        try (InputStream input = file.getInputStream()) {
            fileId = storage.store(input, name, file.getContentType(), applicationId, borrowerId);
        } catch (IOException ex) {
            throw ApiException.badRequest("UNREADABLE_FILE", "The uploaded file could not be read");
        }
        // GridFS and PostgreSQL do not share a transaction. Undo the upload if SQL rolls back.
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override public void afterCompletion(int status) {
                    if (status != STATUS_COMMITTED) cleanup(fileId);
                }
            });
        }
        try {
            LoanDocument document = new LoanDocument();
            document.setLoanApplicationId(applicationId);
            document.setDocType(type);
            document.setFileName(name);
            document.setContentType(file.getContentType());
            document.setSizeBytes(file.getSize());
            document.setGridfsFileId(fileId);
            document.setUploadedAt(clock.instant());
            document = documents.saveAndFlush(document);
            if (application.getStatus() == LoanStatus.PENDING_DOCUMENTS)
                stateMachine.move(application, LoanStatus.SUBMITTED, borrowerId, "Borrower uploaded " + type);
            return DtoMapper.document(document);
        } catch (RuntimeException ex) {
            if (!TransactionSynchronizationManager.isSynchronizationActive()) cleanup(fileId);
            throw ex;
        }
    }

    @Transactional(readOnly = true)
    public Download download(AuthUser caller, UUID documentId) {
        LoanDocument document = documents.findById(documentId).orElseThrow(() -> ApiException.notFound("Document"));
        LoanApplication application = applications.findById(document.getLoanApplicationId())
                .orElseThrow(() -> ApiException.notFound("Application"));
        if (!caller.isStaff() && !caller.id().equals(application.getApplicantId()))
            throw ApiException.notFound("Document");
        if (document.getGridfsFileId() == null) throw ApiException.notFound("Document file");
        return new Download(document.getFileName(), document.getContentType(), document.getSizeBytes(),
                storage.open(document.getGridfsFileId()));
    }

    private void cleanup(String fileId) {
        try { storage.delete(fileId); }
        catch (RuntimeException ex) { log.warn("GridFS cleanup needed for file {}", fileId); }
    }

    private static String safeFilename(String name) {
        if (name == null) return "document";
        String cleaned = name.replace('\\', '/');
        cleaned = cleaned.substring(cleaned.lastIndexOf('/') + 1).replaceAll("[\\p{Cntrl}]", "_").trim();
        if (cleaned.isBlank()) cleaned = "document";
        return cleaned.substring(0, Math.min(200, cleaned.length()));
    }

    private static void validate(MultipartFile file) {
        if (file.isEmpty()) throw ApiException.badRequest("EMPTY_FILE", "Choose a non-empty file");
        if (file.getSize() > MAX_FILE_BYTES)
            throw new ApiException(HttpStatus.PAYLOAD_TOO_LARGE, "FILE_TOO_LARGE", "Files must be PDF/JPG/PNG under 5MB");
        byte[] header;
        try (InputStream input = file.getInputStream()) { header = input.readNBytes(8); }
        catch (IOException ex) { throw ApiException.badRequest("UNREADABLE_FILE", "The uploaded file could not be read"); }
        boolean valid = switch (file.getContentType() == null ? "" : file.getContentType()) {
            case "application/pdf" -> startsWith(header, new byte[]{37, 80, 68, 70, 45});
            case "image/jpeg" -> startsWith(header, new byte[]{(byte)255, (byte)216, (byte)255});
            case "image/png" -> startsWith(header, new byte[]{(byte)137,80,78,71,13,10,26,10});
            default -> false;
        };
        if (!valid) throw ApiException.badRequest("INVALID_FILE_TYPE", "Choose a PDF, JPEG or PNG file matching its content type");
    }

    private static boolean startsWith(byte[] bytes, byte[] signature) {
        return bytes.length >= signature.length && Arrays.equals(Arrays.copyOf(bytes, signature.length), signature);
    }

    public record Download(String filename, String contentType, long size, InputStream stream) {}
}
