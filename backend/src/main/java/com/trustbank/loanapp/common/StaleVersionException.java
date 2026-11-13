package com.trustbank.loanapp.common;

import org.springframework.http.HttpStatus;

/**
 * Optimistic concurrency conflict: the client acted on version N but the row is already at N+k
 * (another officer got there first). Fails fast with 409, holding no locks.
 */
public class StaleVersionException extends ApiException {

    private final long currentVersion;

    public StaleVersionException(long currentVersion) {
        super(HttpStatus.CONFLICT, "VERSION_CONFLICT",
                "This record was changed by someone else. Reload it and try again.");
        this.currentVersion = currentVersion;
    }

    public long getCurrentVersion() {
        return currentVersion;
    }
}
