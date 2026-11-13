package com.trustbank.loanapp.service;

import com.trustbank.loanapp.common.ApiException;
import com.trustbank.loanapp.config.AppProperties;
import com.trustbank.loanapp.domain.ApplicantProfile;
import com.trustbank.loanapp.domain.Approval;
import com.trustbank.loanapp.domain.CheckResult;
import com.trustbank.loanapp.domain.CreditCheck;
import com.trustbank.loanapp.domain.Decision;
import com.trustbank.loanapp.domain.LoanApplication;
import com.trustbank.loanapp.domain.LoanDocument;
import com.trustbank.loanapp.domain.LoanProduct;
import com.trustbank.loanapp.domain.LoanStatus;
import com.trustbank.loanapp.domain.Role;
import com.trustbank.loanapp.domain.User;
import com.trustbank.loanapp.domain.VerificationStatus;
import com.trustbank.loanapp.event.AuditEvent;
import com.trustbank.loanapp.repo.ApplicantProfileRepository;
import com.trustbank.loanapp.repo.ApprovalRepository;
import com.trustbank.loanapp.repo.CreditCheckRepository;
import com.trustbank.loanapp.repo.LoanApplicationRepository;
import com.trustbank.loanapp.repo.LoanDocumentRepository;
import com.trustbank.loanapp.repo.LoanProductRepository;
import com.trustbank.loanapp.repo.UserRepository;
import com.trustbank.loanapp.security.AuthUser;
import com.trustbank.loanapp.web.dto.DtoMapper;
import com.trustbank.loanapp.web.dto.LoanDtos.ApplicationDto;
import com.trustbank.loanapp.web.dto.LoanDtos.CreditCheckRequest;
import com.trustbank.loanapp.web.dto.LoanDtos.DecisionRequest;
import com.trustbank.loanapp.web.dto.LoanDtos.DocumentDto;
import com.trustbank.loanapp.web.dto.LoanDtos.DocumentVerificationRequest;
import com.trustbank.loanapp.web.dto.LoanDtos.StatusChangeRequest;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Staff-side steps of the loan workflow. Every command is one short transaction guarded by the
 * version the officer was looking at, so two officers working the same file can never both win and
 * neither ever waits on the other's lock.
 */
@Service
public class LoanWorkflowService {

    /** Which staff roles may push a loan INTO each manually-set status. Immutable after class init. */
    private static final Map<LoanStatus, Set<Role>> MANUAL_TARGETS = new EnumMap<>(LoanStatus.class);

    static {
        MANUAL_TARGETS.put(LoanStatus.PENDING_DOCUMENTS, EnumSet.of(Role.LOAN_OFFICER, Role.CREDIT_ANALYST));
        MANUAL_TARGETS.put(LoanStatus.DOCUMENTS_REJECTED, EnumSet.of(Role.LOAN_OFFICER, Role.CREDIT_ANALYST));
        MANUAL_TARGETS.put(LoanStatus.CREDIT_CHECK_PENDING, EnumSet.of(Role.LOAN_OFFICER, Role.CREDIT_ANALYST));
        MANUAL_TARGETS.put(LoanStatus.AWAITING_ADDITIONAL_INFO,
                EnumSet.of(Role.LOAN_OFFICER, Role.CREDIT_ANALYST, Role.BRANCH_MANAGER));
        MANUAL_TARGETS.put(LoanStatus.DEFAULTED, EnumSet.of(Role.BRANCH_MANAGER));
    }

    private final LoanApplicationRepository applications;
    private final LoanDocumentRepository documents;
    private final CreditCheckRepository creditChecks;
    private final ApprovalRepository approvals;
    private final LoanProductRepository products;
    private final ApplicantProfileRepository profiles;
    private final UserRepository users;
    private final LoanStateMachine stateMachine;
    private final CreditScoringService scoring;
    private final ApplicationEventPublisher events;
    private final AppProperties props;
    private final Clock clock;

    public LoanWorkflowService(LoanApplicationRepository applications, LoanDocumentRepository documents,
            CreditCheckRepository creditChecks, ApprovalRepository approvals, LoanProductRepository products,
            ApplicantProfileRepository profiles, UserRepository users, LoanStateMachine stateMachine,
            CreditScoringService scoring, ApplicationEventPublisher events, AppProperties props, Clock clock) {
        this.applications = applications;
        this.documents = documents;
        this.creditChecks = creditChecks;
        this.approvals = approvals;
        this.products = products;
        this.profiles = profiles;
        this.users = users;
        this.stateMachine = stateMachine;
        this.scoring = scoring;
        this.events = events;
        this.props = props;
        this.clock = clock;
    }

    @Transactional
    public ApplicationDto assignToMe(AuthUser officer, UUID applicationId, Long version) {
        LoanApplication application = load(applicationId);
        stateMachine.requireVersion(application, version);
        if (application.getAssignedOfficerId() != null && !application.getAssignedOfficerId().equals(officer.id())) {
            throw ApiException.conflict("ALREADY_ASSIGNED", "Another officer already picked up this application");
        }
        application.setAssignedOfficerId(officer.id());
        application.setUpdatedAt(clock.instant());
        events.publishEvent(new AuditEvent("LOAN_APPLICATION", applicationId, "ASSIGNED", officer.id(),
                Map.of("reference", application.getReference())));
        return reply(application);
    }

    @Transactional
    public ApplicationDto changeStatus(AuthUser staff, UUID applicationId, StatusChangeRequest request) {
        Set<Role> allowed = MANUAL_TARGETS.get(request.status());
        if (allowed == null) {
            throw ApiException.rule("NOT_A_MANUAL_STEP",
                    "A loan cannot be moved to " + request.status() + " with this action");
        }
        if (!allowed.contains(staff.role())) {
            throw ApiException.forbidden("Your role cannot move a loan to " + request.status());
        }
        LoanApplication application = load(applicationId);
        stateMachine.requireVersion(application, request.version());

        String note = request.note() == null ? null : request.note().trim();
        boolean needsNote = request.status() == LoanStatus.PENDING_DOCUMENTS
                || request.status() == LoanStatus.AWAITING_ADDITIONAL_INFO
                || request.status() == LoanStatus.DOCUMENTS_REJECTED;
        if (needsNote && (note == null || note.isEmpty())) {
            throw ApiException.rule("REMARKS_REQUIRED", "Remarks are required for this action");
        }
        if (request.status() == LoanStatus.CREDIT_CHECK_PENDING) {
            long unverified = documents.countByLoanApplicationIdAndVerificationStatus(applicationId,
                    VerificationStatus.PENDING)
                    + documents.countByLoanApplicationIdAndVerificationStatus(applicationId,
                            VerificationStatus.REJECTED);
            if (unverified > 0) {
                throw ApiException.rule("DOCUMENTS_NOT_VERIFIED",
                        "Every document must be verified before the credit check");
            }
        }
        if (needsNote) {
            application.setInfoRequestMessage(note);
        }
        // CREDIT_CHECK_PENDING: the event published here is what wakes the background credit worker.
        stateMachine.move(application, request.status(), staff.id(), note);
        return reply(application);
    }

    @Transactional
    public DocumentDto verifyDocument(AuthUser staff, UUID documentId, DocumentVerificationRequest request) {
        if (request.status() == VerificationStatus.PENDING) {
            throw ApiException.rule("INVALID_STATUS", "A document can only be verified or rejected");
        }
        LoanDocument document = documents.findById(documentId).orElseThrow(() -> ApiException.notFound("Document"));
        if (document.getVersion() != request.version()) {
            throw new com.trustbank.loanapp.common.StaleVersionException(document.getVersion());
        }
        document.setVerificationStatus(request.status());
        document.setVerifiedBy(staff.id());
        documents.flush();
        events.publishEvent(new AuditEvent("DOCUMENT", documentId, "DOCUMENT_" + request.status().name(), staff.id(),
                Map.of("loanApplicationId", document.getLoanApplicationId().toString(),
                        "docType", document.getDocType().name())));
        return DtoMapper.document(document);
    }

    /** A credit analyst records (or overrides) the credit result by hand. */
    @Transactional
    public ApplicationDto recordCreditCheck(AuthUser analyst, UUID applicationId, CreditCheckRequest request) {
        LoanApplication application = load(applicationId);
        stateMachine.requireVersion(application, request.version());
        if (application.getStatus() != LoanStatus.CREDIT_CHECK_PENDING) {
            throw ApiException.rule("NOT_AWAITING_CREDIT_CHECK", "This application is not waiting for a credit check");
        }
        Map<String, Object> raw = new LinkedHashMap<>();
        raw.put("source", "MANUAL");
        applyCreditResult(application, "MANUAL-" + analyst.id().toString().substring(0, 8), request.score(),
                request.result(), raw, analyst.id());
        return reply(application);
    }

    // ---- used by the background credit worker -------------------------------------------------

    /** Short read-only transaction: copy what the bureau needs, then give the connection back. */
    @Transactional(readOnly = true)
    public CreditBureauClient.Query creditQuery(UUID applicationId) {
        LoanApplication application = load(applicationId);
        ApplicantProfile profile = profiles.findByUserId(application.getApplicantId())
                .orElseThrow(() -> ApiException.notFound("Profile"));
        return new CreditBureauClient.Query(applicationId, application.getAmountRequested(),
                application.getInterestRateAnnual(), application.getTermMonths(), profile.getIncomeMonthly(),
                profile.getEmploymentInfo().get("employmentType"));
    }

    /**
     * Short write transaction after the slow bureau call has returned. Returns false when there is
     * nothing to do any more (an analyst already recorded a result by hand).
     */
    @Transactional
    public boolean recordAutomatedCreditCheck(UUID applicationId, CreditBureauClient.Report report) {
        LoanApplication application = load(applicationId);
        if (application.getStatus() != LoanStatus.CREDIT_CHECK_PENDING) {
            return false;
        }
        Map<String, Object> raw = new LinkedHashMap<>();
        raw.put("source", "BUREAU_STUB");
        raw.put("score", report.score());
        applyCreditResult(application, report.bureauReference(), report.score(), scoring.resultFor(report.score()),
                raw, null);
        return true;
    }

    private void applyCreditResult(LoanApplication application, String bureauReference, int score,
            CheckResult result, Map<String, Object> raw, UUID checkedBy) {
        CreditCheck check = new CreditCheck();
        check.setLoanApplicationId(application.getId());
        check.setBureauReference(bureauReference);
        check.setScore(score);
        check.setResult(result);
        check.setRawResponse(raw);
        check.setCheckedBy(checkedBy);
        check.setCheckedAt(clock.instant());
        creditChecks.save(check);

        if (result == CheckResult.PASSED) {
            stateMachine.move(application, LoanStatus.CREDIT_CHECK_PASSED, checkedBy, "Score " + score);
            stateMachine.move(application, LoanStatus.UNDER_REVIEW, checkedBy, "Queued for final decision");
        } else {
            stateMachine.move(application, LoanStatus.CREDIT_CHECK_FAILED, checkedBy, "Score " + score);
            application.setDecisionRemarks("Credit check below the required score");
            stateMachine.move(application, LoanStatus.REJECTED, checkedBy, "Credit check failed");
        }
    }

    // ---- final decision -----------------------------------------------------------------------

    /**
     * The branch manager's approve/reject. If two managers open the same file and both click, the
     * first commit bumps the version; the second gets 409 VERSION_CONFLICT immediately.
     */
    @Transactional
    public ApplicationDto decide(AuthUser manager, UUID applicationId, DecisionRequest request) {
        LoanApplication application = load(applicationId);
        stateMachine.requireVersion(application, request.version());
        if (application.getStatus() != LoanStatus.UNDER_REVIEW) {
            throw ApiException.rule("NOT_UNDER_REVIEW", "Only applications under review can be decided");
        }
        String comments = request.comments() == null ? null : request.comments().trim();
        Instant now = clock.instant();

        if (request.decision() == Decision.REJECTED) {
            if (comments == null || comments.isEmpty()) {
                throw ApiException.rule("REMARKS_REQUIRED", "Remarks are required for this action");
            }
            application.setDecisionRemarks(comments);
            stateMachine.move(application, LoanStatus.REJECTED, manager.id(), comments);
        } else {
            BigDecimal approved = request.amountApproved() == null ? application.getAmountRequested()
                    : request.amountApproved();
            LoanProduct product = products.findById(application.getProductId())
                    .orElseThrow(() -> ApiException.notFound("Product"));
            if (approved.compareTo(product.getMinAmount()) < 0 || approved.compareTo(product.getMaxAmount()) > 0) {
                throw ApiException.rule("AMOUNT_OUT_OF_RANGE", "Approved amount is outside the product limits");
            }
            application.setAmountApproved(approved);
            application.setDecisionRemarks(comments);
            application.setOfferExpiresAt(now.plus(props.offer().acceptanceWindow()));   // firm deadline starts
            stateMachine.move(application, LoanStatus.APPROVED, manager.id(), comments);
        }

        Approval approval = new Approval();
        approval.setLoanApplicationId(applicationId);
        approval.setDecidedBy(manager.id());
        approval.setDecision(request.decision());
        approval.setComments(comments);
        approval.setDecidedAt(now);
        approvals.save(approval);
        return reply(application);
    }

    /** Called by the sweeper for offers whose firm deadline passed without an acceptance. */
    @Transactional
    public void expireOffer(UUID applicationId) {
        LoanApplication application = load(applicationId);
        if (application.getStatus() != LoanStatus.APPROVED || application.getOfferExpiresAt() == null
                || clock.instant().isBefore(application.getOfferExpiresAt())) {
            return;
        }
        application.setDecisionRemarks("Offer expired without acceptance");
        stateMachine.move(application, LoanStatus.REJECTED, null, "Offer expired");
    }

    private LoanApplication load(UUID applicationId) {
        return applications.findById(applicationId).orElseThrow(() -> ApiException.notFound("Application"));
    }

    /** Flush first so the reply carries the version the next command must quote. */
    private ApplicationDto reply(LoanApplication application) {
        applications.flush();
        User applicant = users.findById(application.getApplicantId()).orElse(null);
        long pending = documents.countByLoanApplicationIdAndVerificationStatus(application.getId(),
                VerificationStatus.PENDING);
        return DtoMapper.applicationForStaff(application, applicant, pending);
    }
}
