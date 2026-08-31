package com.supersohee.api.admin.error;

import com.supersohee.api.event.controller.AdminEventController;
import com.supersohee.api.player.controller.AdminProfileController;
import com.supersohee.api.guestbook.controller.AdminGuestbookController;
import com.supersohee.api.article.controller.AdminArticleController;
import com.supersohee.api.image.admin.controller.AdminPhotoController;
import com.supersohee.api.playerstat.controller.AdminPlayerStatController;
import com.supersohee.api.schedule.controller.AdminScheduleController;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@RestControllerAdvice(assignableTypes = {
        AdminEventController.class,
        AdminScheduleController.class,
        AdminPlayerStatController.class,
        AdminProfileController.class,
        AdminGuestbookController.class,
        AdminArticleController.class,
        AdminPhotoController.class
})
public class AdminApiExceptionHandler {

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

    @ExceptionHandler({
            HttpMessageNotReadableException.class,
            MissingServletRequestParameterException.class,
            IllegalArgumentException.class
    })
    ResponseEntity<AdminErrorResponse> handleBadRequest() {
        return error(HttpStatus.BAD_REQUEST, "ADMIN_INVALID_REQUEST", "The request is invalid.", Map.of());
    }

    @ExceptionHandler(DuplicateKeyException.class)
    ResponseEntity<AdminErrorResponse> handleDuplicate() {
        return error(HttpStatus.CONFLICT, "ADMIN_RESOURCE_CONFLICT", "A resource with the same unique key already exists.", Map.of());
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<AdminErrorResponse> handleUnexpected() {
        return error(HttpStatus.INTERNAL_SERVER_ERROR, "ADMIN_OPERATION_FAILED", "The admin operation failed.", Map.of());
    }

    private ResponseEntity<AdminErrorResponse> error(
            HttpStatus status,
            String code,
            String message,
            Map<String, String> fieldErrors) {
        return ResponseEntity.status(status).body(new AdminErrorResponse(
                status.value(), code, message, UUID.randomUUID().toString(), fieldErrors));
    }
}
