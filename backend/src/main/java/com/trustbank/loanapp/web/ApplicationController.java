package com.trustbank.loanapp.web;

import com.trustbank.loanapp.domain.LoanStatus;
import com.trustbank.loanapp.idempotency.IdempotencyService;
import com.trustbank.loanapp.idempotency.IdempotentResult;
import com.trustbank.loanapp.security.AuthUser;
import com.trustbank.loanapp.security.CurrentUser;
import com.trustbank.loanapp.service.LoanApplicationService;
import com.trustbank.loanapp.service.RepaymentService;
import com.trustbank.loanapp.web.dto.LoanDtos.ApplicationDetailDto;
import com.trustbank.loanapp.web.dto.LoanDtos.ApplicationDto;
import com.trustbank.loanapp.web.dto.LoanDtos.DocumentDto;
import com.trustbank.loanapp.web.dto.LoanDtos.InfoResponseRequest;
import com.trustbank.loanapp.web.dto.LoanDtos.RepaymentReceiptDto;
import com.trustbank.loanapp.web.dto.LoanDtos.RepaymentRequest;
import com.trustbank.loanapp.web.dto.LoanDtos.ScheduleItemDto;
import com.trustbank.loanapp.web.dto.LoanDtos.SubmitApplicationRequest;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Borrower commands and shared queries.
 *
 * <p>GET is naturally idempotent and safe to retry. The three POSTs that create something or move
 * money are NOT, so they require an {@code Idempotency-Key} header and run through
 * {@link IdempotencyService} (at-most-once).
 */
@RestController
@RequestMapping("/api/v1/applications")
public class ApplicationController {

    private final LoanApplicationService loans;
    private final RepaymentService repayments;
    private final IdempotencyService idempotency;

    public ApplicationController(LoanApplicationService loans, RepaymentService repayments,
            IdempotencyService idempotency) {
        this.loans = loans;
        this.repayments = repayments;
        this.idempotency = idempotency;
    }

    @PostMapping
    @PreAuthorize("hasRole('BORROWER')")
    public ResponseEntity<ApplicationDto> submit(@CurrentUser AuthUser user,
            @RequestHeader(IdempotencyService.HEADER) String idempotencyKey,
            @Valid @RequestBody SubmitApplicationRequest request) {
        IdempotentResult<ApplicationDto> result = idempotency.execute(user.id(), idempotencyKey, "SUBMIT_APPLICATION",
                request, ApplicationDto.class, () -> loans.submit(user.id(), request));
        return reply(result);
    }

    @GetMapping
    public List<ApplicationDto> list(@CurrentUser AuthUser user,
            @RequestParam(required = false) List<LoanStatus> status,
            @RequestParam(defaultValue = "200") int limit) {
        return loans.list(user, status, limit);
    }

    @GetMapping("/{id}")
    public ApplicationDetailDto detail(@CurrentUser AuthUser user, @PathVariable UUID id) {
        return loans.detail(user, id);
    }

    @GetMapping("/{id}/schedule")
    public List<ScheduleItemDto> schedule(@CurrentUser AuthUser user, @PathVariable UUID id) {
        return loans.schedule(user, id);
    }

    @PostMapping("/{id}/info-response")
    @PreAuthorize("hasRole('BORROWER')")
    public ApplicationDto respondToInfoRequest(@CurrentUser AuthUser user, @PathVariable UUID id,
            @Valid @RequestBody InfoResponseRequest request) {
        return loans.respondToInfoRequest(user.id(), id, request.message());
    }

    @PostMapping("/{id}/offer/accept")
    @PreAuthorize("hasRole('BORROWER')")
    public ResponseEntity<ApplicationDto> acceptOffer(@CurrentUser AuthUser user, @PathVariable UUID id,
            @RequestHeader(IdempotencyService.HEADER) String idempotencyKey) {
        IdempotentResult<ApplicationDto> result = idempotency.execute(user.id(), idempotencyKey, "ACCEPT_OFFER",
                Map.of("applicationId", id.toString()), ApplicationDto.class, () -> loans.acceptOffer(user.id(), id));
        return reply(result);
    }

    @PostMapping("/{id}/repayments")
    @PreAuthorize("hasRole('BORROWER')")
    public ResponseEntity<RepaymentReceiptDto> repay(@CurrentUser AuthUser user, @PathVariable UUID id,
            @RequestHeader(IdempotencyService.HEADER) String idempotencyKey,
            @Valid @RequestBody RepaymentRequest request) {
        IdempotentResult<RepaymentReceiptDto> result = idempotency.execute(user.id(), idempotencyKey, "REPAY:" + id,
                request, RepaymentReceiptDto.class, () -> repayments.repay(user.id(), id, request));
        return reply(result);
    }

    /** 201 for the first execution, 200 + "Idempotent-Replayed: true" when the cached reply is returned. */
    static <T> ResponseEntity<T> reply(IdempotentResult<T> result) {
        return ResponseEntity.status(result.replayed() ? HttpStatus.OK : HttpStatus.CREATED)
                .header(IdempotencyService.REPLAYED_HEADER, Boolean.toString(result.replayed()))
                .body(result.body());
    }
}
