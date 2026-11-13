package com.trustbank.loanapp.domain;

import java.util.EnumSet;
import java.util.Set;

/**
 * The loan workflow of diagrams/loan-workflow-state.puml. A loan is one long-running business
 * transaction made of many short database transactions; this table is what keeps it consistent
 * between them: a step is legal only if {@link #canMoveTo(LoanStatus)} says so.
 */
public enum LoanStatus {
    DRAFT, SUBMITTED, PENDING_DOCUMENTS, DOCUMENTS_REJECTED,
    CREDIT_CHECK_PENDING, CREDIT_CHECK_FAILED, CREDIT_CHECK_PASSED,
    UNDER_REVIEW, AWAITING_ADDITIONAL_INFO,
    APPROVED, REJECTED,
    PENDING_DISBURSEMENT, DISBURSED, ACTIVE, DELINQUENT, CLOSED, DEFAULTED;

    public Set<LoanStatus> nextStates() {
        return switch (this) {
            case DRAFT -> EnumSet.of(SUBMITTED);
            case SUBMITTED -> EnumSet.of(PENDING_DOCUMENTS, DOCUMENTS_REJECTED, CREDIT_CHECK_PENDING,
                    AWAITING_ADDITIONAL_INFO);
            case PENDING_DOCUMENTS -> EnumSet.of(SUBMITTED);
            case CREDIT_CHECK_PENDING -> EnumSet.of(CREDIT_CHECK_PASSED, CREDIT_CHECK_FAILED);
            case CREDIT_CHECK_PASSED -> EnumSet.of(UNDER_REVIEW);
            case CREDIT_CHECK_FAILED -> EnumSet.of(REJECTED);
            case UNDER_REVIEW -> EnumSet.of(AWAITING_ADDITIONAL_INFO, APPROVED, REJECTED);
            case AWAITING_ADDITIONAL_INFO -> EnumSet.of(SUBMITTED, UNDER_REVIEW);
            case APPROVED -> EnumSet.of(PENDING_DISBURSEMENT, REJECTED);
            case PENDING_DISBURSEMENT -> EnumSet.of(DISBURSED);
            case DISBURSED -> EnumSet.of(ACTIVE);
            case ACTIVE -> EnumSet.of(DELINQUENT, CLOSED);
            case DELINQUENT -> EnumSet.of(ACTIVE, CLOSED, DEFAULTED);
            case DOCUMENTS_REJECTED, REJECTED, CLOSED, DEFAULTED -> EnumSet.noneOf(LoanStatus.class);
        };
    }

    public boolean canMoveTo(LoanStatus target) {
        return nextStates().contains(target);
    }

    public boolean isTerminal() {
        return nextStates().isEmpty();
    }
}
