package com.supersohee.api.auth.kakao;

import com.supersohee.api.auth.google.GoogleExchangeErrorResponse;
import com.supersohee.api.auth.google.GoogleExchangeException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.UUID;

/**
 * Google 쪽 handler와 같은 오류 형태를 유지한다. 프론트엔드가 provider별로
 * 다른 오류 스키마를 처리하지 않도록 응답 shape을 하나로 둔다.
 */
@RestControllerAdvice(assignableTypes = KakaoAuthExchangeController.class)
@ConditionalOnExpression("!'${kakao.auth.client-id:}'.trim().isEmpty()")
public class KakaoExchangeExceptionHandler {

    @ExceptionHandler(GoogleExchangeException.class)
    public ResponseEntity<GoogleExchangeErrorResponse> handleExchangeError(GoogleExchangeException exception) {
        return error(exception.status(), exception.code(), exception.safeMessage());
    }

    @ExceptionHandler(InvalidKakaoTokenException.class)
    public ResponseEntity<GoogleExchangeErrorResponse> handleInvalidKakaoToken() {
        return error(
                HttpStatus.UNAUTHORIZED,
                "INVALID_KAKAO_TOKEN",
                "The Kakao identity token is invalid or expired.");
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, HttpMessageNotReadableException.class})
    public ResponseEntity<GoogleExchangeErrorResponse> handleInvalidRequest() {
        return error(HttpStatus.BAD_REQUEST, "INVALID_REQUEST", "The exchange request is invalid.");
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<GoogleExchangeErrorResponse> handleUnexpectedError() {
        return error(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "EXCHANGE_FAILED",
                "The exchange could not be completed.");
    }

    private ResponseEntity<GoogleExchangeErrorResponse> error(
            HttpStatus status,
            String code,
            String safeMessage) {
        return ResponseEntity.status(status).body(new GoogleExchangeErrorResponse(
                code,
                status.value(),
                safeMessage,
                UUID.randomUUID().toString()));
    }
}
