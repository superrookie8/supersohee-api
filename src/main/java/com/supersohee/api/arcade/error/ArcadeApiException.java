package com.supersohee.api.arcade.error;

import org.springframework.http.HttpStatus;

public class ArcadeApiException extends RuntimeException {
    private final HttpStatus status;
    private final String code;
    private final String safeMessage;

    private ArcadeApiException(HttpStatus status, String code, String safeMessage) {
        super(code);
        this.status = status;
        this.code = code;
        this.safeMessage = safeMessage;
    }

    public static ArcadeApiException badRequest(String message) {
        return new ArcadeApiException(HttpStatus.BAD_REQUEST, "ARCADE_INVALID_REQUEST", message);
    }

    public static ArcadeApiException userNotFound() {
        return new ArcadeApiException(HttpStatus.NOT_FOUND, "ARCADE_USER_NOT_FOUND", "User was not found.");
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
