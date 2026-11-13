package com.trustbank.loanapp.event;

import com.trustbank.loanapp.config.AsyncConfig;
import com.trustbank.loanapp.domain.Notification;
import com.trustbank.loanapp.repo.NotificationRepository;
import java.time.Clock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Subscriber 1: tells the borrower what happened to their loan.
 *
 * <p>Runs AFTER_COMMIT (never notify about a change that was rolled back) and on the background
 * pool (the officer's HTTP reply does not wait for it).
 */
@Component
public class NotificationSubscriber {

    private static final Logger log = LoggerFactory.getLogger(NotificationSubscriber.class);

    private final NotificationRepository notifications;
    private final Clock clock;

    public NotificationSubscriber(NotificationRepository notifications, Clock clock) {
        this.notifications = notifications;
        this.clock = clock;
    }

    @Async(AsyncConfig.BACKGROUND_EXECUTOR)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onLoanStatusChanged(LoanStatusChangedEvent event) {
        String message = messageFor(event);
        if (message == null) {
            return;
        }
        try {
            Notification notification = new Notification();
            notification.setUserId(event.applicantId());
            notification.setLoanApplicationId(event.applicationId());
            notification.setMessage(message);
            notification.setSentAt(clock.instant());
            notifications.save(notification);
            // A real SMS/e-mail gateway call would go here, still off the request thread.
        } catch (RuntimeException ex) {
            log.error("Could not store notification for loan {}", event.reference(), ex);
        }
    }

    private static String messageFor(LoanStatusChangedEvent event) {
        String ref = event.reference();
        return switch (event.to()) {
            case SUBMITTED -> event.from() == null
                    ? "We have received your loan application " + ref + "."
                    : "Thank you. Your application " + ref + " is back in the review queue.";
            case PENDING_DOCUMENTS -> "Application " + ref + " needs clearer or missing documents. Please upload them.";
            case DOCUMENTS_REJECTED -> "Application " + ref + " could not continue: the documents were not accepted.";
            case UNDER_REVIEW -> "Application " + ref + " passed the credit check and is under review.";
            case AWAITING_ADDITIONAL_INFO -> "The bank needs more information for application " + ref + ".";
            case APPROVED -> "Good news! Application " + ref + " was approved. Accept the offer before it expires.";
            case REJECTED -> "We are sorry. Application " + ref + " was not approved.";
            case PENDING_DISBURSEMENT -> "Offer accepted. Your funds for " + ref + " are being prepared.";
            case ACTIVE -> event.from() == com.trustbank.loanapp.domain.LoanStatus.DELINQUENT
                    ? "Thank you. Loan " + ref + " is up to date again."
                    : "Loan " + ref + " has been disbursed. Your repayment schedule is ready.";
            case DELINQUENT -> "A repayment on loan " + ref + " is overdue. Please pay to avoid penalties.";
            case CLOSED -> "Loan " + ref + " is fully repaid. Thank you for banking with us.";
            case DEFAULTED -> "Loan " + ref + " has been marked as defaulted. Please contact your branch.";
            default -> null; // internal steps (credit check pending/passed/failed, disbursed) stay internal
        };
    }
}
