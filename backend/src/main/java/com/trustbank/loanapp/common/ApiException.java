package com.trustbank.loanapp.common;

import org.springframework.http.HttpStatus;

/** Base class for every expected failure. Carries the HTTP status and a stable error code. */
public class ApiException extends RuntimeException {

    private final HttpStatus status;
    private final String code;

    public ApiException(HttpStatus status, String code, String message) {
        super(message);
        this.status = status;
        this.code = code;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getCode() {
        return code;
    }

    public static ApiException notFound(String what) {
        return new ApiException(HttpStatus.NOT_FOUND, "NOT_FOUND", what + " not found");
    }

    public static ApiException badRequest(String code, String message) {
        return new ApiException(HttpStatus.BAD_REQUEST, code, message);
    }

    public static ApiException forbidden(String message) {
        return new ApiException(HttpStatus.FORBIDDEN, "FORBIDDEN", message);
    }

    /** A rule of the loan business was broken (422): the request is well-formed but not allowed now. */
    public static ApiException rule(String code, String message) {
        return new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, code, message);
    }

    public static ApiException conflict(String code, String message) {
        return new ApiException(HttpStatus.CONFLICT, code, message);
    }

    /** Firm deadline missed (410 Gone): the request has zero value after {@code expires_at}. */
    public static ApiException deadlineExpired(String message) {
        return new ApiException(HttpStatus.GONE, "DEADLINE_EXPIRED", message);
    }
}
