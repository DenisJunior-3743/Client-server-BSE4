package com.trustbank.loanapp.idempotency;

/** {@code replayed} is true when the body came from the reply cache instead of running the action again. */
public record IdempotentResult<T>(T body, boolean replayed) {}
