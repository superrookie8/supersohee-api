package com.supersohee.api.arcade.error;

import com.supersohee.api.arcade.controller.ArcadeController;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@RestControllerAdvice(assignableTypes = ArcadeController.class)
public class ArcadeApiExceptionHandler {

    @ExceptionHandler(ArcadeApiException.class)
    ResponseEntity<ArcadeErrorResponse> handleArcadeException(ArcadeApiException exception) {
        return error(exception.status(), exception.code(), exception.safeMessage(), Map.of());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ArcadeErrorResponse> handleValidation(MethodArgumentNotValidException exception) {
        Map<String, String> fields = new LinkedHashMap<>();
        for (FieldError fieldError : exception.getBindingResult().getFieldErrors()) {
            fields.putIfAbsent(fieldError.getField(), fieldError.getDefaultMessage());
        }
        return error(HttpStatus.BAD_REQUEST, "ARCADE_VALIDATION_FAILED", "Request validation failed.", fields);
    }

    private ResponseEntity<ArcadeErrorResponse> error(
            HttpStatus status,
            String code,
            String message,
            Map<String, String> fieldErrors) {
        return ResponseEntity.status(status).body(new ArcadeErrorResponse(
                status.value(), code, message, UUID.randomUUID().toString(), fieldErrors));
    }
}
