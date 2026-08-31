package com.supersohee.api.user.controller;

import com.supersohee.api.user.service.UserService;
import com.supersohee.api.user.domain.User;
import com.supersohee.api.user.dto.*;
import com.supersohee.api.config.JwtUtil;
import com.supersohee.api.image.service.ImageUploadService;
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
        private final ImageUploadService imageUploadService;

        /**
         * 저장된 profileImageUrl을 화면이 바로 쓸 수 있는 주소로 바꾼다.
         *
         * 직접 올린 사진은 다이어리와 같이 R2 키로 저장하므로 서명된 URL로 변환하고,
         * Google 프로필처럼 이미 절대 URL인 값은 그대로 둔다.
         */
        private UserResponse withDisplayableProfileImage(UserResponse response) {
                String stored = response.getProfileImageUrl();
                if (stored == null || stored.isBlank() || stored.startsWith("http://")
                                || stored.startsWith("https://")) {
                        return response;
                }
                return response.toBuilder()
                                .profileImageUrl(imageUploadService.generatePresignedUrl(stored))
                                .build();
        }

        // 회원가입
        @PostMapping("/signup")
        public ResponseEntity<LoginResponse> signup(@Valid @RequestBody SignupRequest request) {
                User user = userService.signup(
                                request.getEmail(),
                                request.getPassword(),
                                request.getPasswordConfirm(),
                                request.getNickname());

                // JWT 토큰 생성
                String token = jwtUtil.generateUserToken(user.getId());

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
                String token = jwtUtil.generateUserToken(user.getId());

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
                                .map(this::withDisplayableProfileImage)
                                .map(ResponseEntity::ok)
                                .orElse(ResponseEntity.notFound().build());
        }

        // 현재 로그인한 유저의 프로필 수정 (닉네임 / 프로필 사진)
        @PatchMapping("/me")
        public ResponseEntity<UserResponse> updateCurrentUser(
                        @AuthenticationPrincipal String userId,
                        @Valid @RequestBody UpdateMyProfileRequest request) {
                return ResponseEntity.ok(withDisplayableProfileImage(UserResponse.from(
                                userService.updateMyProfile(
                                                userId, request.nickname(), request.profileImageUrl()))));
        }

        // 닉네임 중복 확인. 본인이 쓰고 있는 닉네임은 사용 가능으로 응답한다.
        @GetMapping("/check-nickname")
        public ResponseEntity<Map<String, Boolean>> checkNickname(
                        @AuthenticationPrincipal String userId,
                        @RequestParam(required = false) String nickname) {
                return ResponseEntity.ok(Map.of(
                                "available", userService.isNicknameAvailable(nickname, userId)));
        }

        // 다른 유저 정보 조회 (공개). 개인정보는 담지 않는다.
        @GetMapping("/{userId}")
        public ResponseEntity<PublicUserResponse> getUser(@PathVariable String userId) {
                return userService.findById(userId)
                                .map(PublicUserResponse::from)
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
