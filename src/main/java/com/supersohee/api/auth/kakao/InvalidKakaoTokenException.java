package com.supersohee.api.auth.kakao;

public class InvalidKakaoTokenException extends RuntimeException {
    public InvalidKakaoTokenException() {
        super("Kakao ID token validation failed");
    }

    public InvalidKakaoTokenException(Throwable cause) {
        super("Kakao ID token validation failed", cause);
    }
}
