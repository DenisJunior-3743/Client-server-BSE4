package com.trustbank.loanapp.service;

import com.trustbank.loanapp.common.ApiException;
import com.trustbank.loanapp.common.StaleVersionException;
import com.trustbank.loanapp.domain.LoanApplication;
import com.trustbank.loanapp.domain.LoanStatus;
import com.trustbank.loanapp.domain.LoanStatusHistory;
import com.trustbank.loanapp.event.LoanStatusChangedEvent;
import com.trustbank.loanapp.repo.LoanStatusHistoryRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

/**
 * The single place a loan changes state. Every caller is already inside a transaction, so the
 * status, its history row and the (deferred) event stand or fall together.
 */
@Component
public class LoanStateMachine {

    private final LoanStatusHistoryRepository history;
    private final ApplicationEventPublisher events;
    private final Clock clock;

    public LoanStateMachine(LoanStatusHistoryRepository history, ApplicationEventPublisher events, Clock clock) {
        this.history = history;
        this.events = events;
        this.clock = clock;
    }

    /**
     * Client-side half of optimistic locking: the officer acted on {@code expectedVersion}; if the row
     * has moved on, refuse now. (Hibernate's "WHERE version = ?" covers the race after this check.)
     */
    public void requireVersion(LoanApplication application, Long expectedVersion) {
        if (expectedVersion == null || application.getVersion() != expectedVersion) {
            throw new StaleVersionException(application.getVersion());
        }
    }

    public void move(LoanApplication application, LoanStatus target, UUID actorId, String note) {
        LoanStatus from = application.getStatus();
        if (from != null && !from.canMoveTo(target)) {
            throw ApiException.rule("ILLEGAL_TRANSITION",
                    "A loan that is " + from + " cannot become " + target);
        }
        Instant now = clock.instant();
        application.setStatus(target);
        application.setUpdatedAt(now);

        LoanStatusHistory row = new LoanStatusHistory();
        row.setLoanApplicationId(application.getId());
        row.setFromStatus(from);
        row.setToStatus(target);
        row.setChangedBy(actorId);
        row.setNote(note);
        row.setChangedAt(now);
        history.save(row);

        // Delivered to subscribers only after this transaction commits.
        events.publishEvent(new LoanStatusChangedEvent(application.getId(), application.getReference(),
                application.getApplicantId(), from, target, actorId, note, now));
    }
}
