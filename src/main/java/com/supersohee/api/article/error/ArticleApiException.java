package com.supersohee.api.article.error;

import org.springframework.http.HttpStatus;

import java.util.Map;

public class ArticleApiException extends RuntimeException {
    private final HttpStatus status;
    private final String code;
    private final String safeMessage;
    private final Map<String, String> fieldErrors;

    private ArticleApiException(
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

    public static ArticleApiException validation(Map<String, String> fieldErrors) {
        return new ArticleApiException(
                HttpStatus.BAD_REQUEST,
                "ARTICLE_VALIDATION_FAILED",
                "Article request validation failed.",
                fieldErrors);
    }

    public HttpStatus status() { return status; }
    public String code() { return code; }
    public String safeMessage() { return safeMessage; }
    public Map<String, String> fieldErrors() { return fieldErrors; }
}
