package com.supersohee.api.admin.error;

import org.springframework.http.HttpStatus;

import java.util.Map;

public class AdminApiException extends RuntimeException {
    private final HttpStatus status;
    private final String code;
    private final String safeMessage;
    private final Map<String, String> fieldErrors;

    private AdminApiException(HttpStatus status, String code, String safeMessage) {
        this(status, code, safeMessage, Map.of());
    }

    private AdminApiException(
            HttpStatus status,
            String code,
            String safeMessage,
            Map<String, String> fieldErrors) {
        super(code);
        this.status = status;
        this.code = code;
        this.safeMessage = safeMessage;
        this.fieldErrors = Map.copyOf(fieldErrors);
    }

    public static AdminApiException notFound(String resource) {
        return new AdminApiException(HttpStatus.NOT_FOUND, "ADMIN_RESOURCE_NOT_FOUND", resource + " was not found.");
    }

    public static AdminApiException conflict(String message) {
        return new AdminApiException(HttpStatus.CONFLICT, "ADMIN_RESOURCE_CONFLICT", message);
    }

    public static AdminApiException conflict(String message, Map<String, String> fieldErrors) {
        return new AdminApiException(HttpStatus.CONFLICT, "ADMIN_RESOURCE_CONFLICT", message, fieldErrors);
    }

    public static AdminApiException badRequest(String message) {
        return new AdminApiException(HttpStatus.BAD_REQUEST, "ADMIN_INVALID_REQUEST", message);
    }

    public static AdminApiException unauthorized(String message) {
        return new AdminApiException(HttpStatus.UNAUTHORIZED, "ADMIN_AUTHENTICATION_REQUIRED", message);
    }

    public static AdminApiException payloadTooLarge(String message) {
        return new AdminApiException(HttpStatus.PAYLOAD_TOO_LARGE, "ADMIN_PAYLOAD_TOO_LARGE", message);
    }

    public static AdminApiException unsupportedMediaType(String message) {
        return new AdminApiException(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "ADMIN_UNSUPPORTED_MEDIA_TYPE", message);
    }

    public static AdminApiException storageFailure(String message) {
        return new AdminApiException(HttpStatus.BAD_GATEWAY, "ADMIN_STORAGE_OPERATION_FAILED", message);
    }

    public static AdminApiException validation(String message, Map<String, String> fieldErrors) {
        return new AdminApiException(HttpStatus.BAD_REQUEST, "ADMIN_VALIDATION_FAILED", message, fieldErrors);
    }

    public HttpStatus status() { return status; }
    public String code() { return code; }
    public String safeMessage() { return safeMessage; }
    public Map<String, String> fieldErrors() { return fieldErrors; }
}
