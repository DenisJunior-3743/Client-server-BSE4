package com.trustbank.loanapp.service;

import com.trustbank.loanapp.common.ApiException;
import com.trustbank.loanapp.domain.Disbursement;
import com.trustbank.loanapp.domain.LoanApplication;
import com.trustbank.loanapp.domain.LoanStatus;
import com.trustbank.loanapp.domain.RepaymentSchedule;
import com.trustbank.loanapp.repo.DisbursementRepository;
import com.trustbank.loanapp.repo.LoanApplicationRepository;
import com.trustbank.loanapp.repo.RepaymentScheduleRepository;
import com.trustbank.loanapp.security.AuthUser;
import com.trustbank.loanapp.web.dto.DtoMapper;
import com.trustbank.loanapp.web.dto.LoanDtos.DisburseRequest;
import com.trustbank.loanapp.web.dto.LoanDtos.DisbursementDto;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Releases the money. Three independent guards make sure it happens at most once per loan:
 * the Idempotency-Key (retries), the version check (two managers), and
 * UNIQUE (loan_application_id) on disbursements (anything that slipped past both).
 */
@Service
public class DisbursementService {

    private final LoanApplicationRepository applications;
    private final DisbursementRepository disbursements;
    private final RepaymentScheduleRepository schedules;
    private final LoanStateMachine stateMachine;
    private final Clock clock;

    public DisbursementService(LoanApplicationRepository applications, DisbursementRepository disbursements,
            RepaymentScheduleRepository schedules, LoanStateMachine stateMachine, Clock clock) {
        this.applications = applications;
        this.disbursements = disbursements;
        this.schedules = schedules;
        this.stateMachine = stateMachine;
        this.clock = clock;
    }

    /**
     * One atomic unit: disbursement row + repayment schedule + status change. Either the borrower
     * has the money AND a schedule AND an ACTIVE loan, or none of the three.
     */
    @Transactional
    public DisbursementDto disburse(AuthUser manager, UUID applicationId, DisburseRequest request) {
        LoanApplication application = applications.findById(applicationId)
                .orElseThrow(() -> ApiException.notFound("Application"));
        stateMachine.requireVersion(application, request.version());
        if (application.getStatus() != LoanStatus.PENDING_DISBURSEMENT) {
            throw ApiException.rule("NOT_READY_FOR_DISBURSEMENT",
                    "The borrower has not accepted the offer on this application");
        }
        if (disbursements.existsByLoanApplicationId(applicationId)) {
            throw ApiException.conflict("ALREADY_DISBURSED", "This loan was already disbursed");
        }

        Instant now = clock.instant();
        Disbursement disbursement = new Disbursement();
        disbursement.setLoanApplicationId(applicationId);
        disbursement.setAmount(application.getAmountApproved());
        disbursement.setMethod(request.method());
        disbursement.setStatus("COMPLETED");
        disbursement.setDisbursedBy(manager.id());
        disbursement.setDisbursedAt(now);
        disbursement = disbursements.save(disbursement);

        LocalDate firstDue = LocalDate.ofInstant(now, ZoneOffset.UTC).plusMonths(1);
        List<RepaymentSchedule> rows = new ArrayList<>();
        for (AmortizationCalculator.Installment installment : AmortizationCalculator.schedule(
                application.getAmountApproved(), application.getInterestRateAnnual(), application.getTermMonths())) {
            RepaymentSchedule row = new RepaymentSchedule();
            row.setLoanApplicationId(applicationId);
            row.setInstallmentNo(installment.number());
            row.setDueDate(firstDue.plusMonths(installment.number() - 1L));
            row.setPrincipalDue(installment.principal());
            row.setInterestDue(installment.interest());
            row.setAmountDue(installment.total());
            rows.add(row);
        }
        schedules.saveAll(rows);          // batched INSERTs (hibernate.jdbc.batch_size)

        stateMachine.move(application, LoanStatus.DISBURSED, manager.id(), "Funds released by " + request.method());
        stateMachine.move(application, LoanStatus.ACTIVE, manager.id(), "Repayment schedule started");
        return DtoMapper.disbursement(disbursement);
    }
}
