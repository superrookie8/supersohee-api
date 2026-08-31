package com.supersohee.api.user.error;

import com.supersohee.api.user.controller.UserController;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.NoSuchElementException;
import java.util.Map;

/**
 * 마이페이지 프로필 수정이 실패한 이유를 화면이 구분할 수 있도록 상태 코드로 나눈다.
 * 예외 메시지에 사용자 입력을 되싣지 않는다.
 */
@RestControllerAdvice(assignableTypes = UserController.class)
public class UserApiExceptionHandler {

    @ExceptionHandler(NicknameAlreadyUsedException.class)
    ResponseEntity<Map<String, String>> handleDuplicateNickname(NicknameAlreadyUsedException exception) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of("message", exception.getMessage()));
    }

    @ExceptionHandler(InvalidNicknameException.class)
    ResponseEntity<Map<String, String>> handleInvalidNickname(InvalidNicknameException exception) {
        return ResponseEntity.badRequest().body(Map.of("message", exception.getMessage()));
    }

    @ExceptionHandler(NoSuchElementException.class)
    ResponseEntity<Map<String, String>> handleMissingUser(NoSuchElementException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("message", "사용자를 찾을 수 없습니다."));
    }
}
