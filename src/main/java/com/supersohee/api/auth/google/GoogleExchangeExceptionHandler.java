package com.supersohee.api.auth.google;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.UUID;

@RestControllerAdvice(assignableTypes = GoogleAuthExchangeController.class)
public class GoogleExchangeExceptionHandler {

    @ExceptionHandler(GoogleExchangeException.class)
    public ResponseEntity<GoogleExchangeErrorResponse> handleExchangeError(GoogleExchangeException exception) {
        return error(exception.status(), exception.code(), exception.safeMessage());
    }

    @ExceptionHandler(InvalidGoogleTokenException.class)
    public ResponseEntity<GoogleExchangeErrorResponse> handleInvalidGoogleToken() {
        return error(
                HttpStatus.UNAUTHORIZED,
                "INVALID_GOOGLE_TOKEN",
                "The Google identity token is invalid or expired.");
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, HttpMessageNotReadableException.class})
    public ResponseEntity<GoogleExchangeErrorResponse> handleInvalidRequest() {
        return error(HttpStatus.BAD_REQUEST, "INVALID_REQUEST", "The exchange request is invalid.");
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<GoogleExchangeErrorResponse> handleUnexpectedError() {
        return error(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "EXCHANGE_FAILED",
                "The exchange could not be completed.");
    }

    private ResponseEntity<GoogleExchangeErrorResponse> error(
            HttpStatus status,
            String code,
            String safeMessage) {
        return ResponseEntity.status(status).body(new GoogleExchangeErrorResponse(
                code,
                status.value(),
                safeMessage,
                UUID.randomUUID().toString()));
    }
}
