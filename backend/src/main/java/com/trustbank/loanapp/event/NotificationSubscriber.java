package com.trustbank.loanapp.event;

import com.trustbank.loanapp.messaging.LoanMessageStore;
import com.trustbank.loanapp.messaging.NotificationProcessor;
import jakarta.persistence.EntityManager;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/** Broker-free setup mode: use the same durable events and deduplication procedure in the loan transaction. */
@Component
@ConditionalOnProperty(name = "app.messaging.enabled", havingValue = "false", matchIfMissing = true)
public class NotificationSubscriber {
    private final EntityManager entities;
    private final LoanMessageStore store;
    private final NotificationProcessor processor;

    public NotificationSubscriber(EntityManager entities, LoanMessageStore store, NotificationProcessor processor) {
        this.entities = entities;
        this.store = store;
        this.processor = processor;
    }

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void onLoanStatusChanged(LoanStatusChangedEvent event) {
        entities.flush(); // run history triggers before reading the outbox on the same connection
        for (var id : store.pendingForApplication(event.applicationId())) {
            processor.consume(id);
            store.completeEmbedded(id);
        }
    }
}
