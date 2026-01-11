package com.supersohee.api.admin.controller;

import com.supersohee.api.config.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminAuthController {

    private final JwtUtil jwtUtil;

    @Value("${admin.username:admin}") // 프로덕션에서는 환경 변수로 설정 권장
    private String adminUsername;

    @Value("${admin.password}") // 환경 변수 필수 (기본값 없음)
    private String adminPassword;

    // 어드민 로그인
    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> adminLogin(@RequestBody AdminLoginRequest request) {
        // 정해진 username/password 확인
        if (!adminUsername.equals(request.getUsername()) || !adminPassword.equals(request.getPassword())) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "아이디 또는 비밀번호가 올바르지 않습니다");
            return ResponseEntity.status(401).body(error);
        }

        // 어드민용 JWT 토큰 생성 (userId 대신 "admin" 사용)
        String token = jwtUtil.generateToken("admin");

        Map<String, Object> response = new HashMap<>();
        response.put("token", token);
        response.put("username", adminUsername);
        response.put("role", "ADMIN");

        return ResponseEntity.ok(response);
    }

    // 어드민 로그인 요청 DTO
    @lombok.Getter
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class AdminLoginRequest {
        private String username;
        private String password;
    }
}
