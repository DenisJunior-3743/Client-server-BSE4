package com.trustbank.loanapp.messaging;

import com.trustbank.loanapp.domain.LoanStatus;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NotificationProcessor {
    private final LoanMessageStore store;

    public NotificationProcessor(LoanMessageStore store) {
        this.store = store;
    }

    @Transactional
    public void consume(UUID eventId) {
        consumeStatus(eventId, false);
    }

    @Transactional
    public void consumeStatusFromBroker(UUID eventId) {
        consumeStatus(eventId, true);
    }

    private void consumeStatus(UUID eventId, boolean detectCompositeEvents) {
        var event = store.load(eventId);
        if (event == null) return;
        store.recordNotification(eventId, NotificationText.messageFor(event));
        if (detectCompositeEvents && event.from() == LoanStatus.ACTIVE && event.to() == LoanStatus.DELINQUENT) {
            store.detectRepeatDelinquency(eventId);
        }
    }

    @Transactional
    public void consumeRepeatDelinquency(UUID eventId) {
        store.recordRepeatDelinquencyAlert(eventId);
    }
}
