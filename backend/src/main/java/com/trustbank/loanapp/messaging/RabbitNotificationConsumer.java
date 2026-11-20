package com.trustbank.loanapp.messaging;

import java.nio.charset.StandardCharsets;
import java.util.UUID;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.messaging.enabled", havingValue = "true")
public class RabbitNotificationConsumer {
    private final NotificationProcessor processor;

    public RabbitNotificationConsumer(NotificationProcessor processor) { this.processor = processor; }

    @RabbitListener(queues = RabbitMessagingConfig.QUEUE,
            autoStartup = "${app.messaging.worker-enabled:false}")
    public void consume(Message message) {
        if (message.getBody().length != 36) throw new IllegalArgumentException("Expected a UUID loan message");
        UUID id = UUID.fromString(new String(message.getBody(), StandardCharsets.UTF_8));
        processor.consume(id); // separate transactional bean commits before this listener returns/ACKs
    }
}
