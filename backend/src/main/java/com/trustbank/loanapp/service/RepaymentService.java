package com.trustbank.loanapp.service;

import com.trustbank.loanapp.common.ApiException;
import com.trustbank.loanapp.domain.InstallmentStatus;
import com.trustbank.loanapp.domain.LoanApplication;
import com.trustbank.loanapp.domain.LoanStatus;
import com.trustbank.loanapp.domain.Repayment;
import com.trustbank.loanapp.domain.RepaymentSchedule;
import com.trustbank.loanapp.repo.LoanApplicationRepository;
import com.trustbank.loanapp.repo.RepaymentRepository;
import com.trustbank.loanapp.repo.RepaymentScheduleRepository;
import com.trustbank.loanapp.web.dto.LoanDtos.RepaymentReceiptDto;
import com.trustbank.loanapp.web.dto.LoanDtos.RepaymentRequest;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RepaymentService {

    private final LoanApplicationRepository applications;
    private final RepaymentScheduleRepository schedules;
    private final RepaymentRepository repayments;
    private final LoanStateMachine stateMachine;
    private final Clock clock;

    public RepaymentService(LoanApplicationRepository applications, RepaymentScheduleRepository schedules,
            RepaymentRepository repayments, LoanStateMachine stateMachine, Clock clock) {
        this.applications = applications;
        this.schedules = schedules;
        this.repayments = repayments;
        this.stateMachine = stateMachine;
        this.clock = clock;
    }

    /**
     * Applies one payment to the oldest unpaid installments. Called through IdempotencyService: a
     * retried payment request returns the first receipt instead of taking the money twice.
     *
     * <p>Two different payments racing on the same loan are serialised without a lock: both touch
     * the loan row, so the slower one fails its version check (409) and the client retries.
     */
    @Transactional
    public RepaymentReceiptDto repay(UUID borrowerId, UUID applicationId, RepaymentRequest request) {
        LoanApplication application = applications.findById(applicationId)
                .filter(a -> a.getApplicantId().equals(borrowerId))
                .orElseThrow(() -> ApiException.notFound("Application"));
        if (application.getStatus() != LoanStatus.ACTIVE && application.getStatus() != LoanStatus.DELINQUENT) {
            throw ApiException.rule("LOAN_NOT_REPAYABLE", "This loan is not open for repayment");
        }

        List<RepaymentSchedule> rows = schedules.findByLoanApplicationIdOrderByInstallmentNoAsc(applicationId);
        BigDecimal outstanding = rows.stream().map(RepaymentSchedule::outstanding)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (request.amount().compareTo(outstanding) > 0) {
            throw ApiException.rule("OVERPAYMENT",
                    "The amount is more than the outstanding balance of " + outstanding.toBigInteger());
        }

        Instant now = clock.instant();
        LocalDate today = LocalDate.ofInstant(now, ZoneOffset.UTC);
        String reference = "PAY-" + UUID.randomUUID().toString().substring(0, 13).toUpperCase();
        BigDecimal remaining = request.amount();

        for (RepaymentSchedule row : rows) {
            if (remaining.signum() == 0) {
                break;
            }
            BigDecimal due = row.outstanding();
            if (due.signum() == 0) {
                continue;
            }
            BigDecimal portion = remaining.min(due);
            row.setAmountPaid(row.getAmountPaid().add(portion));
            row.setStatus(row.outstanding().signum() == 0 ? InstallmentStatus.PAID : InstallmentStatus.PARTIAL);
            remaining = remaining.subtract(portion);

            Repayment payment = new Repayment();
            payment.setScheduleId(row.getId());
            payment.setPaymentReference(reference);
            payment.setAmount(portion);
            payment.setMethod(request.method());
            payment.setPaidBy(borrowerId);
            payment.setPaidAt(now);
            repayments.save(payment);
        }

        BigDecimal balance = outstanding.subtract(request.amount());
        application.setUpdatedAt(now);                     // always touch the loan row: bumps its version
        if (balance.signum() == 0) {
            stateMachine.move(application, LoanStatus.CLOSED, borrowerId, "Loan fully repaid");
        } else if (application.getStatus() == LoanStatus.DELINQUENT && !hasOverdue(rows, today)) {
            stateMachine.move(application, LoanStatus.ACTIVE, borrowerId, "Arrears cleared");
        }
        return new RepaymentReceiptDto(reference, request.amount(), balance, application.getStatus(), now);
    }

    /** Called by the sweeper: an ACTIVE loan with an unpaid installment past its due date. */
    @Transactional
    public void markDelinquent(UUID applicationId) {
        LoanApplication application = applications.findById(applicationId).orElse(null);
        if (application == null || application.getStatus() != LoanStatus.ACTIVE) {
            return;
        }
        LocalDate today = LocalDate.ofInstant(clock.instant(), ZoneOffset.UTC);
        List<RepaymentSchedule> rows = schedules.findByLoanApplicationIdOrderByInstallmentNoAsc(applicationId);
        boolean overdue = false;
        for (RepaymentSchedule row : rows) {
            if (row.getStatus() != InstallmentStatus.PAID && row.getDueDate().isBefore(today)) {
                row.setStatus(InstallmentStatus.OVERDUE);
                overdue = true;
            }
        }
        if (overdue) {
            stateMachine.move(application, LoanStatus.DELINQUENT, null, "Installment overdue");
        }
    }

    private static boolean hasOverdue(List<RepaymentSchedule> rows, LocalDate today) {
        return rows.stream().anyMatch(r -> r.getStatus() != InstallmentStatus.PAID && r.getDueDate().isBefore(today));
    }
}
