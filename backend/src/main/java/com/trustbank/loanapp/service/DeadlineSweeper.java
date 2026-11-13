package com.trustbank.loanapp.service;

import com.trustbank.loanapp.idempotency.IdempotencyService;
import com.trustbank.loanapp.repo.LoanApplicationRepository;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Time-driven steps of the loan workflow. Deadlines are ENFORCED at request time (acceptOffer checks
 * expires_at itself); this sweeper only tidies up afterwards, so it does not matter if it runs late.
 *
 * <p>Each loan is handled in its own short transaction and each step re-checks the loan's state, so
 * the jobs are idempotent: running twice, or on two server instances at once, changes nothing extra.
 * A version conflict just means a request thread got there first, and the item is skipped.
 */
@Component
public class DeadlineSweeper {

    private static final Logger log = LoggerFactory.getLogger(DeadlineSweeper.class);
    private static final PageRequest BATCH = PageRequest.of(0, 200);

    private final LoanApplicationRepository applications;
    private final LoanWorkflowService workflow;
    private final RepaymentService repayments;
    private final IdempotencyService idempotency;
    private final Clock clock;

    public DeadlineSweeper(LoanApplicationRepository applications, LoanWorkflowService workflow,
            RepaymentService repayments, IdempotencyService idempotency, Clock clock) {
        this.applications = applications;
        this.workflow = workflow;
        this.repayments = repayments;
        this.idempotency = idempotency;
        this.clock = clock;
    }

    @Scheduled(fixedDelayString = "PT1M", initialDelayString = "PT30S")
    public void expireStaleOffers() {
        for (UUID id : applications.findIdsOfExpiredOffers(clock.instant(), BATCH)) {
            try {
                workflow.expireOffer(id);
            } catch (RuntimeException ex) {
                log.debug("Skipped offer expiry for {}: {}", id, ex.getMessage());
            }
        }
    }

    @Scheduled(cron = "0 15 1 * * *", zone = "UTC")
    public void flagDelinquentLoans() {
        LocalDate today = LocalDate.now(clock.withZone(ZoneOffset.UTC));
        for (UUID id : applications.findIdsOfActiveLoansWithOverdueInstallments(today, BATCH)) {
            try {
                repayments.markDelinquent(id);
            } catch (RuntimeException ex) {
                log.debug("Skipped delinquency check for {}: {}", id, ex.getMessage());
            }
        }
    }

    @Scheduled(fixedDelayString = "PT1H", initialDelayString = "PT5M")
    public void purgeIdempotencyKeys() {
        int removed = idempotency.purgeExpired();
        if (removed > 0) {
            log.info("Purged {} expired idempotency keys", removed);
        }
    }
}
