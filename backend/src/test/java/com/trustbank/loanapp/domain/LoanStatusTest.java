package com.trustbank.loanapp.domain;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.EnumSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** Guards the workflow in diagrams/loan-workflow-state.puml against accidental edits. */
class LoanStatusTest {

    @Test
    void happyPathIsLegalStepByStep() {
        LoanStatus[] path = {LoanStatus.SUBMITTED, LoanStatus.CREDIT_CHECK_PENDING, LoanStatus.CREDIT_CHECK_PASSED,
                LoanStatus.UNDER_REVIEW, LoanStatus.APPROVED, LoanStatus.PENDING_DISBURSEMENT, LoanStatus.DISBURSED,
                LoanStatus.ACTIVE, LoanStatus.CLOSED};
        for (int i = 1; i < path.length; i++) {
            assertTrue(path[i - 1].canMoveTo(path[i]), path[i - 1] + " -> " + path[i]);
        }
    }

    @Test
    void stepsCannotBeSkipped() {
        assertFalse(LoanStatus.SUBMITTED.canMoveTo(LoanStatus.APPROVED));
        assertFalse(LoanStatus.UNDER_REVIEW.canMoveTo(LoanStatus.DISBURSED));
        assertFalse(LoanStatus.APPROVED.canMoveTo(LoanStatus.ACTIVE));
        assertFalse(LoanStatus.CREDIT_CHECK_FAILED.canMoveTo(LoanStatus.UNDER_REVIEW));
    }

    @Test
    void finishedLoansNeverMoveAgain() {
        for (LoanStatus end : EnumSet.of(LoanStatus.REJECTED, LoanStatus.CLOSED, LoanStatus.DEFAULTED,
                LoanStatus.DOCUMENTS_REJECTED)) {
            assertTrue(end.isTerminal(), end.name());
        }
    }

    @Test
    void everyStatusCanBeReachedFromSubmission() {
        Set<LoanStatus> seen = EnumSet.of(LoanStatus.SUBMITTED);
        Deque<LoanStatus> queue = new ArrayDeque<>(seen);
        while (!queue.isEmpty()) {
            for (LoanStatus next : queue.poll().nextStates()) {
                if (seen.add(next)) {
                    queue.add(next);
                }
            }
        }
        Set<LoanStatus> unreachable = EnumSet.complementOf(EnumSet.copyOf(seen));
        unreachable.remove(LoanStatus.DRAFT); // drafts live on the phone until submitted
        assertTrue(unreachable.isEmpty(), "unreachable: " + unreachable);
    }
}
