package com.supersohee.api.article.error;

import com.supersohee.api.article.controller.ArticleController;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.Map;
import java.util.UUID;

@RestControllerAdvice(assignableTypes = ArticleController.class)
public class ArticleApiExceptionHandler {

    @ExceptionHandler(ArticleApiException.class)
    ResponseEntity<ArticleErrorResponse> handleArticleException(ArticleApiException exception) {
        return error(exception.status(), exception.code(), exception.safeMessage(), exception.fieldErrors());
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    ResponseEntity<ArticleErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException exception) {
        String field = exception.getName();
        return error(
                HttpStatus.BAD_REQUEST,
                "ARTICLE_VALIDATION_FAILED",
                "Article request validation failed.",
                Map.of(field, field + " must be an integer."));
    }

    private ResponseEntity<ArticleErrorResponse> error(
            HttpStatus status,
            String code,
            String message,
            Map<String, String> fieldErrors) {
        return ResponseEntity.status(status).body(new ArticleErrorResponse(
                status.value(), code, message, UUID.randomUUID().toString(), fieldErrors));
    }
}
