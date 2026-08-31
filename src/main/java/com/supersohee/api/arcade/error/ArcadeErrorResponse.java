package com.supersohee.api.arcade.error;

import java.util.Map;

public record ArcadeErrorResponse(
        int status,
        String code,
        String message,
        String traceId,
        Map<String, String> fieldErrors) {
}
