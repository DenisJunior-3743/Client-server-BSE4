package com.trustbank.loanapp.messaging;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableRabbit
@ConditionalOnProperty(name = "app.messaging.enabled", havingValue = "true")
public class RabbitMessagingConfig {
    public static final String EXCHANGE = "trustbank.loan.events.v1";
    public static final String KEY = "loan.status.changed";
    public static final String QUEUE = "trustbank.loan.notifications.v1";
    public static final String DEAD_EXCHANGE = "trustbank.loan.dead.v1";
    public static final String DEAD_QUEUE = "trustbank.loan.notifications.dead.v1";
    public static final String REPEAT_DELINQUENCY_KEY = "loan.delinquency.repeated";
    public static final String STAFF_ALERT_QUEUE = "trustbank.loan.staff-alerts.v1";
    public static final String STAFF_ALERT_DEAD_QUEUE = "trustbank.loan.staff-alerts.dead.v1";

    @Bean public DirectExchange loanExchange() { return new DirectExchange(EXCHANGE, true, false); }
    @Bean public DirectExchange loanDeadExchange() { return new DirectExchange(DEAD_EXCHANGE, true, false); }
    @Bean public Queue loanQueue() {
        return QueueBuilder.durable(QUEUE).deadLetterExchange(DEAD_EXCHANGE).deadLetterRoutingKey(KEY).build();
    }
    @Bean public Queue loanDeadQueue() { return QueueBuilder.durable(DEAD_QUEUE).build(); }
    @Bean public Binding loanBinding() {
        return BindingBuilder.bind(loanQueue()).to(loanExchange()).with(KEY);
    }
    @Bean public Binding loanDeadBinding() {
        return BindingBuilder.bind(loanDeadQueue()).to(loanDeadExchange()).with(KEY);
    }
    @Bean public Queue staffAlertQueue() {
        return QueueBuilder.durable(STAFF_ALERT_QUEUE).deadLetterExchange(DEAD_EXCHANGE)
                .deadLetterRoutingKey(REPEAT_DELINQUENCY_KEY).build();
    }
    @Bean public Queue staffAlertDeadQueue() { return QueueBuilder.durable(STAFF_ALERT_DEAD_QUEUE).build(); }
    @Bean public Binding staffAlertBinding() {
        return BindingBuilder.bind(staffAlertQueue()).to(loanExchange()).with(REPEAT_DELINQUENCY_KEY);
    }
    @Bean public Binding staffAlertDeadBinding() {
        return BindingBuilder.bind(staffAlertDeadQueue()).to(loanDeadExchange()).with(REPEAT_DELINQUENCY_KEY);
    }
}
