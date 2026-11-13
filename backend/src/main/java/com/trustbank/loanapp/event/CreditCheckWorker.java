package com.trustbank.loanapp.event;

import com.trustbank.loanapp.config.AsyncConfig;
import com.trustbank.loanapp.domain.LoanStatus;
import com.trustbank.loanapp.service.CreditBureauClient;
import com.trustbank.loanapp.service.LoanWorkflowService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Subscriber 3: the automated credit check. Content-based subscription: it only cares about events
 * whose new status is CREDIT_CHECK_PENDING.
 *
 * <p>This is delayed concurrency plus deadlock avoidance in one place. The officer's request
 * returned long ago. Here, on a background thread, the work is split in three so that the slow part
 * holds nothing:
 * <ol>
 *   <li>short read transaction: copy the figures the bureau needs, release the connection;</li>
 *   <li>the slow bureau call, with NO transaction, NO connection and NO lock held;</li>
 *   <li>short write transaction: store the result and advance the loan.</li>
 * </ol>
 * A server that kept a transaction open across step 2 would pin a pooled connection (and any row
 * locks) for as long as a remote system felt like taking: the server-deadlock pattern of Comer 8.26.
 */
@Component
public class CreditCheckWorker {

    private static final Logger log = LoggerFactory.getLogger(CreditCheckWorker.class);
    private static final int MAX_ATTEMPTS = 3;

    private final LoanWorkflowService workflow;
    private final CreditBureauClient bureau;

    public CreditCheckWorker(LoanWorkflowService workflow, CreditBureauClient bureau) {
        this.workflow = workflow;
        this.bureau = bureau;
    }

    @Async(AsyncConfig.BACKGROUND_EXECUTOR)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onLoanStatusChanged(LoanStatusChangedEvent event) {
        if (event.to() != LoanStatus.CREDIT_CHECK_PENDING) {
            return;
        }
        try {
            CreditBureauClient.Query query = workflow.creditQuery(event.applicationId());      // 1
            CreditBureauClient.Report report = bureau.check(query);                            // 2
            for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {                        // 3
                try {
                    boolean applied = workflow.recordAutomatedCreditCheck(event.applicationId(), report);
                    log.info("Credit check for {}: score {} ({})", event.reference(), report.score(),
                            applied ? "recorded" : "skipped, already handled");
                    return;
                } catch (OptimisticLockingFailureException conflict) {
                    // Someone touched the loan while we were at the bureau. Re-read and try again.
                    log.debug("Version conflict recording credit check for {}, attempt {}", event.reference(), attempt);
                }
            }
            log.warn("Gave up recording credit check for {} after {} attempts", event.reference(), MAX_ATTEMPTS);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        } catch (RuntimeException ex) {
            // The loan simply stays CREDIT_CHECK_PENDING; an analyst can record the result by hand.
            log.error("Automated credit check failed for {}", event.reference(), ex);
        }
    }
}
