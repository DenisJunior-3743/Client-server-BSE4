package com.trustbank.loanapp.messaging;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.core.ReturnedMessage;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RabbitLoanPublisherTest {
    @Test void routesPersistentCompositeEventAndWaitsForConfirmation() throws Exception {
        var rabbit = mock(RabbitTemplate.class);
        UUID id = UUID.randomUUID();
        doAnswer(inv -> {
            Message msg = inv.getArgument(2);
            assertEquals(id.toString(), new String(msg.getBody(), java.nio.charset.StandardCharsets.UTF_8));
            assertEquals(MessageDeliveryMode.PERSISTENT, msg.getMessageProperties().getDeliveryMode());
            CorrelationData data = inv.getArgument(3);
            data.getFuture().complete(new CorrelationData.Confirm(true, null));
            return null;
        }).when(rabbit).send(eq(RabbitMessagingConfig.EXCHANGE), eq(RabbitMessagingConfig.REPEAT_DELINQUENCY_KEY),
                any(Message.class), any(CorrelationData.class));
        assertDoesNotThrow(() -> new RabbitLoanPublisher(rabbit).publish(
                new LoanMessageStore.ClaimedMessage(id, LoanMessageType.REPEAT_DELINQUENCY)));
    }

    @Test void acknowledgedButUnroutableMessageMustNotBeMarkedPublished() {
        var rabbit = mock(RabbitTemplate.class);
        doAnswer(inv -> {
            CorrelationData data = inv.getArgument(3);
            data.setReturned(new ReturnedMessage(inv.getArgument(2), 312, "NO_ROUTE", "exchange", "key"));
            data.getFuture().complete(new CorrelationData.Confirm(true, null));
            return null;
        }).when(rabbit).send(anyString(), anyString(), any(Message.class), any(CorrelationData.class));
        var event = new LoanMessageStore.ClaimedMessage(UUID.randomUUID(), LoanMessageType.LOAN_STATUS_CHANGED);
        assertThrows(IllegalStateException.class, () -> new RabbitLoanPublisher(rabbit).publish(event));
    }

    @Test void negativeBrokerConfirmationFailsPublication() {
        var rabbit = mock(RabbitTemplate.class);
        doAnswer(inv -> {
            CorrelationData data = inv.getArgument(3);
            data.getFuture().complete(new CorrelationData.Confirm(false, "rejected"));
            return null;
        }).when(rabbit).send(anyString(), anyString(), any(Message.class), any(CorrelationData.class));
        var event = new LoanMessageStore.ClaimedMessage(UUID.randomUUID(), LoanMessageType.LOAN_STATUS_CHANGED);
        assertThrows(IllegalStateException.class, () -> new RabbitLoanPublisher(rabbit).publish(event));
    }
}
