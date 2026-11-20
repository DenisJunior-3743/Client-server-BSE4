package com.trustbank.loanapp.messaging;

import com.trustbank.loanapp.domain.LoanStatus;
import com.trustbank.loanapp.event.LoanStatusChangedEvent;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class NotificationProcessorTest {
    @Test void notificationUsesTrustedDatabaseEvent() {
        var store = mock(LoanMessageStore.class);
        UUID id = UUID.randomUUID();
        when(store.load(id)).thenReturn(new LoanStatusChangedEvent(UUID.randomUUID(), "LN-2026-1000",
                UUID.randomUUID(), LoanStatus.UNDER_REVIEW, LoanStatus.APPROVED, null, null, Instant.now()));
        new NotificationProcessor(store).consume(id);
        verify(store).recordNotification(id,
                "Good news! Application LN-2026-1000 was approved. Accept the offer before it expires.");
    }

    @Test void deletedLoanIsHarmlessOnRedelivery() {
        var store = mock(LoanMessageStore.class);
        new NotificationProcessor(store).consume(UUID.randomUUID());
        verify(store, never()).recordNotification(any(), any());
    }

    @Test void malformedBrokerMessageNeverTouchesDatabase() {
        var processor = mock(NotificationProcessor.class);
        var consumer = new RabbitNotificationConsumer(processor);
        assertThrows(IllegalArgumentException.class, () -> consumer.consume(
                new Message("invalid".getBytes(), new MessageProperties())));
        verifyNoInteractions(processor);
    }
}
