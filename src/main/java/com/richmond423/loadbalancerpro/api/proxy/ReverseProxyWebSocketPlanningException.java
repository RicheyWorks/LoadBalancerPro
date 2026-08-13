package com.richmond423.loadbalancerpro.api.proxy;

import org.springframework.http.HttpStatus;

final class ReverseProxyWebSocketPlanningException extends RuntimeException {
    private final HttpStatus status;
    private final String errorCode;
    private final Integer retryAfterSeconds;

    ReverseProxyWebSocketPlanningException(HttpStatus status, String errorCode, String message) {
        this(status, errorCode, message, null);
    }

    ReverseProxyWebSocketPlanningException(
            HttpStatus status, String errorCode, String message, Integer retryAfterSeconds) {
        super(message);
        this.status = status;
        this.errorCode = errorCode;
        this.retryAfterSeconds = retryAfterSeconds;
    }

    HttpStatus status() {
        return status;
    }

    String errorCode() {
        return errorCode;
    }

    Integer retryAfterSeconds() {
        return retryAfterSeconds;
    }
}
