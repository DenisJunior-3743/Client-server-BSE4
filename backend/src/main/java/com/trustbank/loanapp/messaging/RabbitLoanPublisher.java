package com.trustbank.loanapp.messaging;

import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/** Publish IDs only: the worker loads the trusted payload from PostgreSQL, not untrusted broker JSON. */
@Component
@ConditionalOnProperty(name = "app.messaging.enabled", havingValue = "true")
public class RabbitLoanPublisher {
    private final RabbitTemplate rabbit;

    public RabbitLoanPublisher(RabbitTemplate rabbit) { this.rabbit = rabbit; }

    public void publish(LoanMessageStore.ClaimedMessage message) throws Exception {
        UUID id = message.id();
        var properties = new MessageProperties();
        properties.setMessageId(id.toString());
        properties.setContentType(MessageProperties.CONTENT_TYPE_TEXT_PLAIN);
        properties.setDeliveryMode(MessageDeliveryMode.PERSISTENT);
        var correlation = new CorrelationData(id.toString());
        rabbit.send(RabbitMessagingConfig.EXCHANGE, message.type().routingKey,
                new Message(id.toString().getBytes(StandardCharsets.UTF_8), properties), correlation);
        var confirm = correlation.getFuture().get(5, TimeUnit.SECONDS);
        if (!confirm.isAck() || correlation.getReturned() != null) {
            throw new IllegalStateException("Broker did not accept and route the loan message");
        }
    }
}
