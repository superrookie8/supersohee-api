package com.supersohee.api.auth.google;

public record GoogleExchangeErrorResponse(
        String code,
        int status,
        String message,
        String traceId) {
}
