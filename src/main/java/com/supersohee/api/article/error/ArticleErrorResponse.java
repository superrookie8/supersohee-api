package com.supersohee.api.article.error;

import java.util.Map;

public record ArticleErrorResponse(
        int status,
        String code,
        String message,
        String traceId,
        Map<String, String> fieldErrors) {
}
