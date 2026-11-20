package com.trustbank.loanapp.messaging;

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
        var event = store.load(eventId);
        if (event == null) return;
        store.recordNotification(eventId, NotificationText.messageFor(event));
    }
}
