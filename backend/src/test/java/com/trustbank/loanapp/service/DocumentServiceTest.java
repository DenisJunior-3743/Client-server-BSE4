package com.trustbank.loanapp.service;

import com.trustbank.loanapp.common.ApiException;
import com.trustbank.loanapp.domain.*;
import com.trustbank.loanapp.repo.*;
import com.trustbank.loanapp.security.AuthUser;
import com.trustbank.loanapp.storage.DocumentStorage;
import java.io.ByteArrayInputStream;
import java.time.Clock;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class DocumentServiceTest {
    final LoanApplicationRepository applications = mock(LoanApplicationRepository.class);
    final LoanDocumentRepository documents = mock(LoanDocumentRepository.class);
    final DocumentStorage storage = mock(DocumentStorage.class);
    final LoanStateMachine machine = mock(LoanStateMachine.class);
    final DocumentService service = new DocumentService(applications, documents, machine, storage, Clock.systemUTC());
    final UUID borrower = UUID.randomUUID(), applicationId = UUID.randomUUID(), documentId = UUID.randomUUID();
    final String fileId = "507f1f77bcf86cd799439011";
    final MockMultipartFile pdf = new MockMultipartFile("file", "proof.pdf", "application/pdf", "%PDF-1.4 test".getBytes());
    LoanApplication application;

    @BeforeEach void setup() {
        application = new LoanApplication();
        application.setId(applicationId);
        application.setApplicantId(borrower);
        application.setStatus(LoanStatus.SUBMITTED);
        when(applications.findById(applicationId)).thenReturn(Optional.of(application));
        when(storage.store(any(), anyString(), anyString(), any(), any())).thenReturn(fileId);
        when(documents.saveAndFlush(any())).thenAnswer(invocation -> {
            LoanDocument document = invocation.getArgument(0); document.setId(documentId); return document;
        });
    }

    @Test void uploadStoresPrivateGridfsReferenceAndReturnsAuthenticatedDownloadPath() {
        var result = service.upload(borrower, applicationId, DocumentType.NATIONAL_ID, pdf);
        assertEquals("/api/v1/documents/" + documentId + "/content", result.downloadUrl());
        assertEquals(pdf.getSize(), result.sizeBytes());
        verify(documents).saveAndFlush(argThat(d -> fileId.equals(d.getGridfsFileId())
                && d.getFileUrl() == null && d.getVerificationStatus() == VerificationStatus.PENDING));
    }
    @Test void borrowerCannotUploadToAnotherBorrowersApplication() {
        assertEquals(404, assertThrows(ApiException.class, () -> service.upload(UUID.randomUUID(), applicationId,
                DocumentType.NATIONAL_ID, pdf)).getStatus().value());
        verifyNoInteractions(storage);
    }
    @Test void closedApplicationsRejectUploads() {
        application.setStatus(LoanStatus.ACTIVE);
        assertThrows(ApiException.class, () -> service.upload(borrower, applicationId, DocumentType.NATIONAL_ID, pdf));
        verifyNoInteractions(storage);
    }
    @Test void rejectsEmptyOversizedAndDisguisedFilesBeforeStorage() {
        for (var file : new MockMultipartFile[]{
                new MockMultipartFile("file", "x.pdf", "application/pdf", new byte[0]),
                new MockMultipartFile("file", "x.pdf", "application/pdf", new byte[5_242_881]),
                new MockMultipartFile("file", "x.pdf", "application/pdf", "<script>bad</script>".getBytes()),
                new MockMultipartFile("file", "x.exe", "application/octet-stream", "%PDF-1.4".getBytes())}) {
            assertThrows(ApiException.class, () -> service.upload(borrower, applicationId, DocumentType.PAYSLIP, file));
        }
        verifyNoInteractions(storage);
    }
    @Test void sqlFailureDeletesUploadedFileWhenNoTransactionSynchronizationIsPresent() {
        doThrow(new IllegalStateException("SQL unavailable")).when(documents).saveAndFlush(any());
        assertThrows(IllegalStateException.class, () -> service.upload(borrower, applicationId, DocumentType.PAYSLIP, pdf));
        verify(storage).delete(fileId);
    }
    @Test void transactionRollbackDeletesUploadedFileButCommitRetainsIt() {
        for (int completion : new int[]{TransactionSynchronization.STATUS_COMMITTED, TransactionSynchronization.STATUS_ROLLED_BACK}) {
            reset(storage);
            when(storage.store(any(), anyString(), anyString(), any(), any())).thenReturn(fileId);
            TransactionSynchronizationManager.initSynchronization();
            try {
                service.upload(borrower, applicationId, DocumentType.PAYSLIP, pdf);
                for (var sync : TransactionSynchronizationManager.getSynchronizations()) sync.afterCompletion(completion);
                verify(storage, times(completion == TransactionSynchronization.STATUS_COMMITTED ? 0 : 1)).delete(fileId);
            } finally { TransactionSynchronizationManager.clearSynchronization(); }
        }
    }
    @Test void downloadsAreRestrictedToOwnerAndStaff() {
        LoanDocument document = new LoanDocument(); document.setId(documentId);
        document.setLoanApplicationId(applicationId); document.setGridfsFileId(fileId);
        when(documents.findById(documentId)).thenReturn(Optional.of(document));
        var otherBorrower = new AuthUser(UUID.randomUUID(), "other@example.com", Role.BORROWER);
        assertEquals(404, assertThrows(ApiException.class, () -> service.download(otherBorrower, documentId)).getStatus().value());
        verify(storage, never()).open(anyString());
        when(storage.open(fileId)).thenReturn(new ByteArrayInputStream(new byte[0]));
        assertNotNull(service.download(new AuthUser(borrower, "owner@example.com", Role.BORROWER), documentId));
        assertNotNull(service.download(new AuthUser(UUID.randomUUID(), "staff@example.com", Role.LOAN_OFFICER), documentId));
        verify(storage, times(2)).open(fileId);
    }
    @Test void reuploadRequeuesPendingDocuments() {
        application.setStatus(LoanStatus.PENDING_DOCUMENTS);
        service.upload(borrower, applicationId, DocumentType.BANK_STATEMENT, pdf);
        verify(machine).move(eq(application), eq(LoanStatus.SUBMITTED), eq(borrower), anyString());
    }
}
