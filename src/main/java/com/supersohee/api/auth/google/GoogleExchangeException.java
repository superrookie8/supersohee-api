package com.supersohee.api.auth.google;

import org.springframework.http.HttpStatus;

public class GoogleExchangeException extends RuntimeException {
    private final HttpStatus status;
    private final String code;
    private final String safeMessage;

    private GoogleExchangeException(HttpStatus status, String code, String safeMessage) {
        super(code);
        this.status = status;
        this.code = code;
        this.safeMessage = safeMessage;
    }

    public static GoogleExchangeException unauthorized() {
        return new GoogleExchangeException(
                HttpStatus.UNAUTHORIZED,
                "EXCHANGE_UNAUTHORIZED",
                "The exchange request is not authorized.");
    }

    public static GoogleExchangeException invalidRequest() {
        return new GoogleExchangeException(
                HttpStatus.BAD_REQUEST,
                "INVALID_REQUEST",
                "The exchange request is invalid.");
    }

    public static GoogleExchangeException replayDetected() {
        return new GoogleExchangeException(
                HttpStatus.CONFLICT,
                "TOKEN_REPLAY_DETECTED",
                "This identity token has already been exchanged.");
    }

    public static GoogleExchangeException idempotencyConflict() {
        return new GoogleExchangeException(
                HttpStatus.CONFLICT,
                "IDEMPOTENCY_CONFLICT",
                "The idempotency key was already used for another request.");
    }

    public static GoogleExchangeException exchangeInProgress() {
        return new GoogleExchangeException(
                HttpStatus.CONFLICT,
                "EXCHANGE_IN_PROGRESS",
                "An exchange using this idempotency key is already in progress.");
    }

    public static GoogleExchangeException rateLimited() {
        return new GoogleExchangeException(
                HttpStatus.TOO_MANY_REQUESTS,
                "RATE_LIMITED",
                "Too many exchange requests. Please retry later.");
    }

    public HttpStatus status() {
        return status;
    }

    public String code() {
        return code;
    }

    public String safeMessage() {
        return safeMessage;
    }
}
