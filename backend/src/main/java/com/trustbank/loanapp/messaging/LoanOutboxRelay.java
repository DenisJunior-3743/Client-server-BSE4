package com.trustbank.loanapp.messaging;

import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.messaging.enabled", havingValue = "true")
public class LoanOutboxRelay {
    private static final Logger log = LoggerFactory.getLogger(LoanOutboxRelay.class);
    private final LoanMessageStore store;
    private final RabbitLoanPublisher publisher;
    private final boolean worker;

    public LoanOutboxRelay(LoanMessageStore store, RabbitLoanPublisher publisher,
            @Value("${app.messaging.worker-enabled:false}") boolean worker) {
        this.store = store;
        this.publisher = publisher;
        this.worker = worker;
    }

    @Scheduled(fixedDelayString = "PT5S", initialDelayString = "PT10S")
    public void scheduledPublish() {
        if (worker) publishBatch();
    }

    public int publishBatch() {
        UUID token = UUID.randomUUID();
        var ids = store.claim(token); // transaction finishes before network I/O
        int sent = 0;
        for (UUID id : ids) {
            try {
                publisher.publish(id);
                store.complete(id, token); // only after publisher confirm AND successful routing
                sent++;
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                break; // remaining leases expire; another worker recovers them
            } catch (Exception ex) {
                store.retryLater(id, token);
                log.warn("Loan message {} will retry ({})", id, ex.getClass().getSimpleName());
            }
        }
        return sent;
    }
}
