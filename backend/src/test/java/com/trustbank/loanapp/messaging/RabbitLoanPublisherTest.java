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
    @Test void publishesPersistentIdAndWaitsForConfirmation() throws Exception {
        var rabbit = mock(RabbitTemplate.class);
        UUID id = UUID.randomUUID();
        doAnswer(inv -> {
            Message msg = inv.getArgument(2);
            assertEquals(id.toString(), new String(msg.getBody(), java.nio.charset.StandardCharsets.UTF_8));
            assertEquals(MessageDeliveryMode.PERSISTENT, msg.getMessageProperties().getDeliveryMode());
            CorrelationData data = inv.getArgument(3);
            data.getFuture().complete(new CorrelationData.Confirm(true, null));
            return null;
        }).when(rabbit).send(eq(RabbitMessagingConfig.EXCHANGE), eq(RabbitMessagingConfig.KEY),
                any(Message.class), any(CorrelationData.class));
        assertDoesNotThrow(() -> new RabbitLoanPublisher(rabbit).publish(id));
    }

    @Test void acknowledgedButUnroutableMessageMustNotBeMarkedPublished() {
        var rabbit = mock(RabbitTemplate.class);
        doAnswer(inv -> {
            CorrelationData data = inv.getArgument(3);
            data.setReturned(new ReturnedMessage(inv.getArgument(2), 312, "NO_ROUTE", "exchange", "key"));
            data.getFuture().complete(new CorrelationData.Confirm(true, null));
            return null;
        }).when(rabbit).send(anyString(), anyString(), any(Message.class), any(CorrelationData.class));
        assertThrows(IllegalStateException.class, () -> new RabbitLoanPublisher(rabbit).publish(UUID.randomUUID()));
    }

    @Test void negativeBrokerConfirmationFailsPublication() {
        var rabbit = mock(RabbitTemplate.class);
        doAnswer(inv -> {
            CorrelationData data = inv.getArgument(3);
            data.getFuture().complete(new CorrelationData.Confirm(false, "rejected"));
            return null;
        }).when(rabbit).send(anyString(), anyString(), any(Message.class), any(CorrelationData.class));
        assertThrows(IllegalStateException.class, () -> new RabbitLoanPublisher(rabbit).publish(UUID.randomUUID()));
    }
}
