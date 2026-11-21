package com.trustbank.loanapp.messaging;

enum LoanMessageType {
    LOAN_STATUS_CHANGED(RabbitMessagingConfig.KEY),
    REPEAT_DELINQUENCY(RabbitMessagingConfig.REPEAT_DELINQUENCY_KEY);

    final String routingKey;

    LoanMessageType(String routingKey) {
        this.routingKey = routingKey;
    }
}
