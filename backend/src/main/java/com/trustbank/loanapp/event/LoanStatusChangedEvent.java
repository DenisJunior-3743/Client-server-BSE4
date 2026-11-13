package com.trustbank.loanapp.event;

import com.trustbank.loanapp.domain.LoanStatus;
import java.time.Instant;
import java.util.UUID;

/**
 * Published on the topic "loan status changed" whenever a loan moves to a new state
 * (LOAN_APPROVED, LOAN_REJECTED, ... are simply values of {@code to}).
 *
 * <p>The publisher (a service) does not know who is listening. Subscribers filter on content
 * ({@code to}) and react on their own threads, after the publisher's transaction has committed.
 * An immutable record, so it is safe to hand between threads.
 */
public record LoanStatusChangedEvent(UUID applicationId, String reference, UUID applicantId, LoanStatus from,
        LoanStatus to, UUID actorId, String note, Instant occurredAt) {}
