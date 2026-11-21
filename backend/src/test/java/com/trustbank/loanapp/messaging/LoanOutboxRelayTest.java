package com.trustbank.loanapp.messaging;

import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class LoanOutboxRelayTest {
    @Test void confirmedPublicationCompletesTheClaim() throws Exception {
        var store = mock(LoanMessageStore.class);
        var publisher = mock(RabbitLoanPublisher.class);
        UUID id = UUID.randomUUID();
        var message = new LoanMessageStore.ClaimedMessage(id, LoanMessageType.REPEAT_DELINQUENCY);
        when(store.claim(any())).thenReturn(List.of(message));
        assertEquals(1, new LoanOutboxRelay(store, publisher, true).publishBatch());
        var order = inOrder(store, publisher);
        var token = ArgumentCaptor.forClass(UUID.class);
        order.verify(store).claim(token.capture());
        order.verify(publisher).publish(message);
        order.verify(store).complete(id, token.getValue());
        verify(store, never()).retryLater(any(), any());
    }

    @Test void brokerFailureRetainsEventForRetryAndContinuesBatch() throws Exception {
        var store = mock(LoanMessageStore.class);
        var publisher = mock(RabbitLoanPublisher.class);
        UUID first = UUID.randomUUID(), second = UUID.randomUUID();
        var firstMessage = new LoanMessageStore.ClaimedMessage(first, LoanMessageType.LOAN_STATUS_CHANGED);
        var secondMessage = new LoanMessageStore.ClaimedMessage(second, LoanMessageType.REPEAT_DELINQUENCY);
        when(store.claim(any())).thenReturn(List.of(firstMessage, secondMessage));
        doThrow(new IllegalStateException("unavailable")).when(publisher).publish(firstMessage);
        assertEquals(1, new LoanOutboxRelay(store, publisher, true).publishBatch());
        verify(store).retryLater(eq(first), any());
        verify(store, never()).complete(eq(first), any());
        verify(store).complete(eq(second), any());
    }

    @Test void vercelApiDoesNotRunBackgroundRelay() {
        var store = mock(LoanMessageStore.class);
        var publisher = mock(RabbitLoanPublisher.class);
        new LoanOutboxRelay(store, publisher, false).scheduledPublish();
        verifyNoInteractions(store, publisher);
    }

    @Test void shutdownPreservesInterruptAndLeavesClaimsRecoverable() throws Exception {
        var store = mock(LoanMessageStore.class);
        var publisher = mock(RabbitLoanPublisher.class);
        UUID id = UUID.randomUUID();
        var message = new LoanMessageStore.ClaimedMessage(id, LoanMessageType.LOAN_STATUS_CHANGED);
        when(store.claim(any())).thenReturn(List.of(message));
        doThrow(new InterruptedException()).when(publisher).publish(message);
        try {
            assertEquals(0, new LoanOutboxRelay(store, publisher, true).publishBatch());
            assertTrue(Thread.currentThread().isInterrupted());
            verify(store, never()).complete(any(), any());
        } finally { Thread.interrupted(); }
    }
}
