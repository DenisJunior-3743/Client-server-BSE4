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
        when(store.claim(any())).thenReturn(List.of(id));
        assertEquals(1, new LoanOutboxRelay(store, publisher, true).publishBatch());
        var order = inOrder(store, publisher);
        var token = ArgumentCaptor.forClass(UUID.class);
        order.verify(store).claim(token.capture());
        order.verify(publisher).publish(id);
        order.verify(store).complete(id, token.getValue());
        verify(store, never()).retryLater(any(), any());
    }

    @Test void brokerFailureRetainsEventForRetryAndContinuesBatch() throws Exception {
        var store = mock(LoanMessageStore.class);
        var publisher = mock(RabbitLoanPublisher.class);
        UUID first = UUID.randomUUID(), second = UUID.randomUUID();
        when(store.claim(any())).thenReturn(List.of(first, second));
        doThrow(new IllegalStateException("unavailable")).when(publisher).publish(first);
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
        when(store.claim(any())).thenReturn(List.of(id));
        doThrow(new InterruptedException()).when(publisher).publish(id);
        try {
            assertEquals(0, new LoanOutboxRelay(store, publisher, true).publishBatch());
            assertTrue(Thread.currentThread().isInterrupted());
            verify(store, never()).complete(any(), any());
        } finally { Thread.interrupted(); }
    }
}
