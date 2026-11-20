package com.trustbank.loanapp.messaging;

import com.trustbank.loanapp.event.LoanStatusChangedEvent;

/** Shared by embedded delivery and RabbitMQ; never place credentials or file bytes in a message. */
public final class NotificationText {
    private NotificationText() {}

    public static String messageFor(LoanStatusChangedEvent event) {
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
