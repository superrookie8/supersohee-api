package com.supersohee.api.admin.error;

import java.util.Map;

public record AdminErrorResponse(
        int status,
        String code,
        String message,
        String traceId,
        Map<String, String> fieldErrors) {
}
