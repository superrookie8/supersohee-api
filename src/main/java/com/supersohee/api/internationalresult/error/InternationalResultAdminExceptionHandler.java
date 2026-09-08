package com.supersohee.api.internationalresult.error;

import com.supersohee.api.admin.error.AdminApiException;
import com.supersohee.api.admin.error.AdminErrorResponse;
import com.supersohee.api.internationalresult.controller.AdminInternationalResultController;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@RestControllerAdvice(assignableTypes = AdminInternationalResultController.class)
public class InternationalResultAdminExceptionHandler {
    @ExceptionHandler(AdminApiException.class)
    ResponseEntity<AdminErrorResponse> handleAdminException(AdminApiException exception) {
        return error(exception.status(), exception.code(), exception.safeMessage(), exception.fieldErrors());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<AdminErrorResponse> handleValidation(MethodArgumentNotValidException exception) {
        Map<String, String> fields = new LinkedHashMap<>();
        for (FieldError fieldError : exception.getBindingResult().getFieldErrors()) {
            fields.putIfAbsent(fieldError.getField(), fieldError.getDefaultMessage());
        }
        return error(HttpStatus.BAD_REQUEST, "ADMIN_VALIDATION_FAILED", "Request validation failed.", fields);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<AdminErrorResponse> handleUnreadable() {
        return error(HttpStatus.BAD_REQUEST, "ADMIN_INVALID_REQUEST", "The request is invalid.", Map.of());
    }

    @ExceptionHandler(DuplicateKeyException.class)
    ResponseEntity<AdminErrorResponse> handleDuplicate() {
        return error(HttpStatus.CONFLICT, "ADMIN_RESOURCE_CONFLICT",
                "A resource with the same unique key already exists.", Map.of());
    }

    private ResponseEntity<AdminErrorResponse> error(
            HttpStatus status,
            String code,
            String message,
            Map<String, String> fieldErrors) {
        return ResponseEntity.status(status)
                .cacheControl(CacheControl.noStore().cachePrivate())
                .body(new AdminErrorResponse(status.value(), code, message, UUID.randomUUID().toString(), fieldErrors));
    }
}
