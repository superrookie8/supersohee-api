package com.supersohee.api.auth.google;

public class InvalidGoogleTokenException extends RuntimeException {
    public InvalidGoogleTokenException() {
        super("Google ID token validation failed");
    }

    public InvalidGoogleTokenException(Throwable cause) {
        super("Google ID token validation failed", cause);
    }
}
