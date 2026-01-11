package com.supersohee.api.user.controller;

import com.supersohee.api.user.service.UserService;
import com.supersohee.api.user.domain.User;
import com.supersohee.api.user.dto.*;
import com.supersohee.api.config.JwtUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

        private final UserService userService;
        private final JwtUtil jwtUtil;

        // 회원가입
        @PostMapping("/signup")
        public ResponseEntity<LoginResponse> signup(@Valid @RequestBody SignupRequest request) {
                User user = userService.signup(
                                request.getEmail(),
                                request.getPassword(),
                                request.getPasswordConfirm(),
                                request.getNickname());

                // JWT 토큰 생성
                String token = jwtUtil.generateToken(user.getId());

                return ResponseEntity.ok(new LoginResponse(
                                token,
                                user.getId(),
                                user.getEmail(),
                                user.getNickname()));
        }

        // 로그인
        @PostMapping("/login")
        public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
                User user = userService.login(
                                request.getEmail(),
                                request.getPassword());

                // JWT 토큰 생성
                String token = jwtUtil.generateToken(user.getId());

                return ResponseEntity.ok(new LoginResponse(
                                token,
                                user.getId(),
                                user.getEmail(),
                                user.getNickname()));
        }

        // 현재 로그인한 유저 정보 조회
        @GetMapping("/me")
        public ResponseEntity<UserResponse> getCurrentUser(@AuthenticationPrincipal String userId) {
                return userService.getCurrentUser(userId)
                                .map(UserResponse::from)
                                .map(ResponseEntity::ok)
                                .orElse(ResponseEntity.notFound().build());
        }

        // 유저 정보 조회
        @GetMapping("/{userId}")
        public ResponseEntity<UserResponse> getUser(@PathVariable String userId) {
                return userService.findById(userId)
                                .map(UserResponse::from)
                                .map(ResponseEntity::ok)
                                .orElse(ResponseEntity.notFound().build());
        }

        // 이메일 중복 확인
        @GetMapping("/check-email")
        public ResponseEntity<?> checkEmail(@RequestParam(required = false) String email) {
                try {
                        // 이메일이 없거나 빈 문자열인 경우
                        if (email == null || email.trim().isEmpty()) {
                                Map<String, String> error = Map.of("error", "이메일을 입력해주세요");
                                return ResponseEntity.badRequest().body(error);
                        }

                        // 이메일 형식 간단 검증
                        if (!email.contains("@")) {
                                Map<String, String> error = Map.of("error", "올바른 이메일 형식이 아닙니다");
                                return ResponseEntity.badRequest().body(error);
                        }

                        boolean exists = userService.isEmailExists(email.trim());
                        return ResponseEntity.ok(Map.of("exists", exists));
                } catch (Exception e) {
                        // 예외 발생 시 에러 응답
                        Map<String, String> error = Map.of("error", "이메일 확인 중 오류가 발생했습니다: " + e.getMessage());
                        return ResponseEntity.internalServerError().body(error);
                }
        }
}