package com.trustbank.loanapp.web;

import com.trustbank.loanapp.idempotency.IdempotencyService;
import com.trustbank.loanapp.idempotency.IdempotentResult;
import com.trustbank.loanapp.security.AuthUser;
import com.trustbank.loanapp.security.CurrentUser;
import com.trustbank.loanapp.service.DisbursementService;
import com.trustbank.loanapp.service.LoanWorkflowService;
import com.trustbank.loanapp.web.dto.LoanDtos.ApplicationDto;
import com.trustbank.loanapp.web.dto.LoanDtos.AssignRequest;
import com.trustbank.loanapp.web.dto.LoanDtos.CreditCheckRequest;
import com.trustbank.loanapp.web.dto.LoanDtos.DecisionRequest;
import com.trustbank.loanapp.web.dto.LoanDtos.DisburseRequest;
import com.trustbank.loanapp.web.dto.LoanDtos.DisbursementDto;
import com.trustbank.loanapp.web.dto.LoanDtos.DocumentDto;
import com.trustbank.loanapp.web.dto.LoanDtos.DocumentVerificationRequest;
import com.trustbank.loanapp.web.dto.LoanDtos.StatusChangeRequest;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Staff commands. Authorization duty: each step names the role(s) allowed to take it, and a borrower
 * token is refused on every one of them.
 *
 * <p>These commands are safe to retry without an Idempotency-Key because each one carries the
 * expected {@code version}: a duplicate of an already-applied command no longer matches the row's
 * version and is refused with 409 instead of being applied twice. Disbursement moves money, so it
 * gets the key as well.
 */
@RestController
@RequestMapping("/api/v1")
public class StaffWorkflowController {

    private final LoanWorkflowService workflow;
    private final DisbursementService disbursements;
    private final IdempotencyService idempotency;

    public StaffWorkflowController(LoanWorkflowService workflow, DisbursementService disbursements,
            IdempotencyService idempotency) {
        this.workflow = workflow;
        this.disbursements = disbursements;
        this.idempotency = idempotency;
    }

    @PostMapping("/applications/{id}/assign")
    @PreAuthorize("hasRole('LOAN_OFFICER')")
    public ApplicationDto assign(@CurrentUser AuthUser user, @PathVariable UUID id,
            @Valid @RequestBody AssignRequest request) {
        return workflow.assignToMe(user, id, request.version());
    }

    @PostMapping("/applications/{id}/status")
    @PreAuthorize("hasAnyRole('LOAN_OFFICER','CREDIT_ANALYST','BRANCH_MANAGER')")
    public ApplicationDto changeStatus(@CurrentUser AuthUser user, @PathVariable UUID id,
            @Valid @RequestBody StatusChangeRequest request) {
        return workflow.changeStatus(user, id, request);
    }

    @PatchMapping("/documents/{id}/verification")
    @PreAuthorize("hasAnyRole('CREDIT_ANALYST','LOAN_OFFICER')")
    public DocumentDto verifyDocument(@CurrentUser AuthUser user, @PathVariable UUID id,
            @Valid @RequestBody DocumentVerificationRequest request) {
        return workflow.verifyDocument(user, id, request);
    }

    @PostMapping("/applications/{id}/credit-check")
    @PreAuthorize("hasRole('CREDIT_ANALYST')")
    public ApplicationDto recordCreditCheck(@CurrentUser AuthUser user, @PathVariable UUID id,
            @Valid @RequestBody CreditCheckRequest request) {
        return workflow.recordCreditCheck(user, id, request);
    }

    @PostMapping("/applications/{id}/decision")
    @PreAuthorize("hasRole('BRANCH_MANAGER')")
    public ApplicationDto decide(@CurrentUser AuthUser user, @PathVariable UUID id,
            @Valid @RequestBody DecisionRequest request) {
        return workflow.decide(user, id, request);
    }

    @PostMapping("/applications/{id}/disbursement")
    @PreAuthorize("hasRole('BRANCH_MANAGER')")
    public ResponseEntity<DisbursementDto> disburse(@CurrentUser AuthUser user, @PathVariable UUID id,
            @RequestHeader(IdempotencyService.HEADER) String idempotencyKey,
            @Valid @RequestBody DisburseRequest request) {
        IdempotentResult<DisbursementDto> result = idempotency.execute(user.id(), idempotencyKey, "DISBURSE:" + id,
                request, DisbursementDto.class, () -> disbursements.disburse(user, id, request));
        return ApplicationController.reply(result);
    }
}
