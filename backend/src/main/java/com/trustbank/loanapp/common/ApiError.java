package com.trustbank.loanapp.common;

import java.time.Instant;
import java.util.Map;

/** The only error shape the API ever returns. {@code code} is stable and machine-readable. */
public record ApiError(
        Instant timestamp,
        int status,
        String code,
        String message,
        String path,
        Map<String, String> fieldErrors,
        Long currentVersion) {

    public static ApiError of(int status, String code, String message, String path) {
        return new ApiError(Instant.now(), status, code, message, path, null, null);
    }
}
