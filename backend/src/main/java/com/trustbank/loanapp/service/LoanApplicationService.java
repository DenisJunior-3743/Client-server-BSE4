package com.trustbank.loanapp.service;

import com.trustbank.loanapp.common.ApiException;
import com.trustbank.loanapp.domain.ApplicantProfile;
import com.trustbank.loanapp.domain.LoanApplication;
import com.trustbank.loanapp.domain.LoanDocument;
import com.trustbank.loanapp.domain.LoanProduct;
import com.trustbank.loanapp.domain.LoanStatus;
import com.trustbank.loanapp.domain.LoanStatusHistory;
import com.trustbank.loanapp.domain.Role;
import com.trustbank.loanapp.domain.User;
import com.trustbank.loanapp.repo.ApplicantProfileRepository;
import com.trustbank.loanapp.repo.ApprovalRepository;
import com.trustbank.loanapp.repo.CreditCheckRepository;
import com.trustbank.loanapp.repo.DisbursementRepository;
import com.trustbank.loanapp.repo.LoanApplicationRepository;
import com.trustbank.loanapp.repo.LoanDocumentRepository;
import com.trustbank.loanapp.repo.LoanProductRepository;
import com.trustbank.loanapp.repo.LoanStatusHistoryRepository;
import com.trustbank.loanapp.repo.RepaymentScheduleRepository;
import com.trustbank.loanapp.repo.UserRepository;
import com.trustbank.loanapp.security.AuthUser;
import com.trustbank.loanapp.web.dto.DtoMapper;
import com.trustbank.loanapp.web.dto.LoanDtos.ApplicationDetailDto;
import com.trustbank.loanapp.web.dto.LoanDtos.ApplicationDto;
import com.trustbank.loanapp.web.dto.LoanDtos.DocumentDto;
import com.trustbank.loanapp.web.dto.LoanDtos.DocumentRequest;
import com.trustbank.loanapp.web.dto.LoanDtos.ScheduleItemDto;
import com.trustbank.loanapp.web.dto.LoanDtos.SubmitApplicationRequest;
import java.time.Clock;
import java.time.Instant;
import java.time.Year;
import java.time.ZoneOffset;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Borrower-side operations and all read queries.
 *
 * <p>Thread safety (Comer 3.4.2): every field below is a final reference to a stateless collaborator.
 * Nothing about any one borrower or loan is ever stored in a field; request data lives only in
 * method parameters and local variables, which are private to the request's thread.
 */
@Service
public class LoanApplicationService {

    private static final int MAX_PAGE = 500;

    private final LoanApplicationRepository applications;
    private final LoanProductRepository products;
    private final ApplicantProfileRepository profiles;
    private final UserRepository users;
    private final LoanDocumentRepository documents;
    private final CreditCheckRepository creditChecks;
    private final ApprovalRepository approvals;
    private final DisbursementRepository disbursements;
    private final LoanStatusHistoryRepository history;
    private final RepaymentScheduleRepository schedules;
    private final LoanStateMachine stateMachine;
    private final Clock clock;

    public LoanApplicationService(LoanApplicationRepository applications, LoanProductRepository products,
            ApplicantProfileRepository profiles, UserRepository users, LoanDocumentRepository documents,
            CreditCheckRepository creditChecks, ApprovalRepository approvals, DisbursementRepository disbursements,
            LoanStatusHistoryRepository history, RepaymentScheduleRepository schedules,
            LoanStateMachine stateMachine, Clock clock) {
        this.applications = applications;
        this.products = products;
        this.profiles = profiles;
        this.users = users;
        this.documents = documents;
        this.creditChecks = creditChecks;
        this.approvals = approvals;
        this.disbursements = disbursements;
        this.history = history;
        this.schedules = schedules;
        this.stateMachine = stateMachine;
        this.clock = clock;
    }

    // ---- commands (borrower) ------------------------------------------------------------------

    /** Called through IdempotencyService, so a retried submit can never create a second loan. */
    @Transactional
    public ApplicationDto submit(UUID borrowerId, SubmitApplicationRequest request) {
        LoanProduct product = products.findById(request.productId())
                .filter(LoanProduct::isActive)
                .orElseThrow(() -> ApiException.rule("PRODUCT_UNAVAILABLE", "This loan product is not available"));
        if (!profiles.findByUserId(borrowerId).isPresent()) {
            throw ApiException.rule("PROFILE_INCOMPLETE", "Complete your profile before applying for a loan");
        }
        if (request.amountRequested().compareTo(product.getMinAmount()) < 0
                || request.amountRequested().compareTo(product.getMaxAmount()) > 0) {
            throw ApiException.rule("AMOUNT_OUT_OF_RANGE", "Amount must be between "
                    + product.getMinAmount().toBigInteger() + " and " + product.getMaxAmount().toBigInteger());
        }
        if (request.termMonths() < product.getMinTermMonths() || request.termMonths() > product.getMaxTermMonths()) {
            throw ApiException.rule("TERM_OUT_OF_RANGE", "Term must be between " + product.getMinTermMonths()
                    + " and " + product.getMaxTermMonths() + " months");
        }

        Instant now = clock.instant();
        LoanApplication application = new LoanApplication();
        application.setReference("LN-" + Year.now(clock.withZone(ZoneOffset.UTC)) + "-"
                + String.format("%06d", applications.nextReferenceNumber()));
        application.setApplicantId(borrowerId);
        application.setProductId(product.getId());
        application.setProductType(product.getType());
        application.setAmountRequested(request.amountRequested());
        application.setInterestRateAnnual(product.getInterestRateAnnual());   // rate is fixed at application time
        application.setTermMonths(request.termMonths());
        application.setPurpose(request.purpose().trim());
        application.setMetadata(request.details() == null ? new LinkedHashMap<>() : new LinkedHashMap<>(request.details()));
        application.setSubmittedAt(now);
        application.setUpdatedAt(now);
        application.setStatus(LoanStatus.SUBMITTED);
        application = applications.save(application);

        application.setStatus(null);                       // so the history row reads "(start) -> SUBMITTED"
        stateMachine.move(application, LoanStatus.SUBMITTED, borrowerId, null);
        return DtoMapper.applicationForBorrower(application);
    }

    @Transactional
    public DocumentDto addDocument(UUID borrowerId, UUID applicationId, DocumentRequest request) {
        LoanApplication application = ownApplication(borrowerId, applicationId);
        Set<LoanStatus> open = Set.of(LoanStatus.SUBMITTED, LoanStatus.PENDING_DOCUMENTS,
                LoanStatus.AWAITING_ADDITIONAL_INFO);
        if (!open.contains(application.getStatus())) {
            throw ApiException.rule("DOCUMENTS_CLOSED", "Documents can no longer be added to this application");
        }
        LoanDocument document = new LoanDocument();
        document.setLoanApplicationId(applicationId);
        document.setDocType(request.docType());
        document.setFileName(request.fileName().trim());
        document.setContentType(request.contentType());
        document.setSizeBytes(request.sizeBytes());
        document.setUploadedAt(clock.instant());
        document = documents.save(document);

        // Re-upload after a "pending documents" request puts the file back in the officer's queue.
        if (application.getStatus() == LoanStatus.PENDING_DOCUMENTS) {
            stateMachine.move(application, LoanStatus.SUBMITTED, borrowerId, "Borrower uploaded " + request.docType());
        }
        return DtoMapper.document(document);
    }

    @Transactional
    public ApplicationDto respondToInfoRequest(UUID borrowerId, UUID applicationId, String message) {
        LoanApplication application = ownApplication(borrowerId, applicationId);
        if (application.getStatus() != LoanStatus.AWAITING_ADDITIONAL_INFO) {
            throw ApiException.rule("NO_INFO_REQUEST", "The bank is not waiting for information on this application");
        }
        // Go back to wherever the request came from (SUBMITTED or UNDER_REVIEW).
        List<LoanStatusHistory> rows = history.findByLoanApplicationIdOrderByChangedAtAsc(applicationId);
        LoanStatus returnTo = LoanStatus.UNDER_REVIEW;
        for (int i = rows.size() - 1; i >= 0; i--) {
            LoanStatusHistory row = rows.get(i);
            if (row.getToStatus() == LoanStatus.AWAITING_ADDITIONAL_INFO && row.getFromStatus() != null) {
                returnTo = row.getFromStatus();
                break;
            }
        }
        application.setInfoRequestMessage(null);
        stateMachine.move(application, returnTo, borrowerId, message.trim());
        applications.flush();                              // so the reply carries the new version
        return DtoMapper.applicationForBorrower(application);
    }

    /**
     * FIRM deadline: an approved offer must be accepted before {@code offer_expires_at}. One second
     * late is the same as never. Called through IdempotencyService.
     */
    @Transactional
    public ApplicationDto acceptOffer(UUID borrowerId, UUID applicationId) {
        LoanApplication application = ownApplication(borrowerId, applicationId);
        if (application.getStatus() != LoanStatus.APPROVED) {
            throw ApiException.rule("NO_OPEN_OFFER", "There is no open offer on this application");
        }
        Instant now = clock.instant();
        if (application.getOfferExpiresAt() == null || !now.isBefore(application.getOfferExpiresAt())) {
            throw ApiException.deadlineExpired("This loan offer has expired");
        }
        application.setOfferAcceptedAt(now);
        stateMachine.move(application, LoanStatus.PENDING_DISBURSEMENT, borrowerId, "Offer accepted");
        applications.flush();
        return DtoMapper.applicationForBorrower(application);
    }

    // ---- queries ------------------------------------------------------------------------------

    @Transactional(readOnly = true)
    public List<ApplicationDto> list(AuthUser caller, Collection<LoanStatus> statuses, int limit) {
        PageRequest page = PageRequest.of(0, Math.max(1, Math.min(limit, MAX_PAGE)));
        if (!caller.isStaff()) {
            // Authorization by construction: a borrower's query is always scoped to their own id.
            return applications.findByApplicantIdOrderBySubmittedAtDesc(caller.id(), page).stream()
                    .filter(a -> statuses == null || statuses.isEmpty() || statuses.contains(a.getStatus()))
                    .map(DtoMapper::applicationForBorrower)
                    .toList();
        }
        List<LoanApplication> rows = statuses == null || statuses.isEmpty()
                ? applications.findAllByOrderBySubmittedAtDesc(page)
                : applications.findByStatusInOrderBySubmittedAtDesc(statuses, page);
        if (rows.isEmpty()) {
            return List.of();
        }
        // Two set-based lookups for the whole page instead of two per row (no N+1 round trips).
        Map<UUID, User> applicants = users
                .findAllById(rows.stream().map(LoanApplication::getApplicantId).collect(Collectors.toSet())).stream()
                .collect(Collectors.toMap(User::getId, u -> u));
        Map<UUID, Long> pending = new HashMap<>();
        for (Object[] row : documents.countPendingByApplicationIds(rows.stream().map(LoanApplication::getId).toList())) {
            pending.put((UUID) row[0], (Long) row[1]);
        }
        return rows.stream()
                .map(a -> DtoMapper.applicationForStaff(a, applicants.get(a.getApplicantId()),
                        pending.getOrDefault(a.getId(), 0L)))
                .toList();
    }

    @Transactional(readOnly = true)
    public ApplicationDetailDto detail(AuthUser caller, UUID applicationId) {
        LoanApplication application = applications.findById(applicationId)
                .orElseThrow(() -> ApiException.notFound("Application"));
        boolean staff = caller.isStaff();
        if (!staff && !application.getApplicantId().equals(caller.id())) {
            // 404, not 403: do not confirm that someone else's application id exists.
            throw ApiException.notFound("Application");
        }
        List<DocumentDto> docs = documents.findByLoanApplicationIdOrderByUploadedAtAsc(applicationId).stream()
                .map(DtoMapper::document).toList();
        ApplicantProfile profile = profiles.findByUserId(application.getApplicantId()).orElse(null);
        var disbursement = disbursements.findByLoanApplicationId(applicationId).map(DtoMapper::disbursement)
                .orElse(null);
        var timeline = history.findByLoanApplicationIdOrderByChangedAtAsc(applicationId).stream()
                .map(h -> DtoMapper.history(h, staff)).toList();

        if (!staff) {
            // Privacy: the borrower sees their own data and the outcome, not the bank's working notes
            // (credit score, bureau reference, who decided).
            return new ApplicationDetailDto(DtoMapper.applicationForBorrower(application), docs, null, null,
                    disbursement, profile == null ? null : DtoMapper.profile(profile, false), timeline);
        }
        long pendingDocs = docs.stream()
                .filter(d -> d.verificationStatus() == com.trustbank.loanapp.domain.VerificationStatus.PENDING).count();
        // Only the roles that verify identity see the full National ID.
        boolean maskNin = caller.role() == Role.ADMIN || caller.role() == Role.BRANCH_MANAGER;
        return new ApplicationDetailDto(
                DtoMapper.applicationForStaff(application, users.findById(application.getApplicantId()).orElse(null),
                        pendingDocs),
                docs,
                creditChecks.findFirstByLoanApplicationIdOrderByCheckedAtDesc(applicationId)
                        .map(DtoMapper::creditCheck).orElse(null),
                approvals.findFirstByLoanApplicationIdOrderByDecidedAtDesc(applicationId)
                        .map(DtoMapper::approval).orElse(null),
                disbursement,
                profile == null ? null : DtoMapper.profile(profile, maskNin),
                timeline);
    }

    @Transactional(readOnly = true)
    public List<ScheduleItemDto> schedule(AuthUser caller, UUID applicationId) {
        LoanApplication application = applications.findById(applicationId)
                .orElseThrow(() -> ApiException.notFound("Application"));
        if (!caller.isStaff() && !application.getApplicantId().equals(caller.id())) {
            throw ApiException.notFound("Application");
        }
        return schedules.findByLoanApplicationIdOrderByInstallmentNoAsc(applicationId).stream()
                .map(DtoMapper::scheduleItem).toList();
    }

    private LoanApplication ownApplication(UUID borrowerId, UUID applicationId) {
        return applications.findById(applicationId)
                .filter(a -> a.getApplicantId().equals(borrowerId))
                .orElseThrow(() -> ApiException.notFound("Application"));
    }
}
