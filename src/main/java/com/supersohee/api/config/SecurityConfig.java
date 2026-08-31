package com.supersohee.api.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.supersohee.api.article.security.ArticleImportKeyAuthenticationFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfigurationSource;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

        private final JwtAuthenticationFilter jwtAuthenticationFilter;
        private final ArticleImportKeyAuthenticationFilter articleImportKeyAuthenticationFilter;
        private final CorsConfigurationSource corsConfigurationSource;

        // 인증 실패 시 JSON 응답 반환
        @Bean
        public AuthenticationEntryPoint authenticationEntryPoint() {
                return (HttpServletRequest request, HttpServletResponse response,
                                AuthenticationException authException) -> {
                        // OPTIONS 요청은 CORS 헤더만 반환하고 종료
                        if ("OPTIONS".equals(request.getMethod())) {
                                response.setStatus(HttpServletResponse.SC_OK);
                                return;
                        }

                        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                        response.setCharacterEncoding("UTF-8");

                        Map<String, Object> error = new HashMap<>();
                        if (request.getRequestURI().startsWith("/api/admin/")) {
                                error.put("status", 401);
                                error.put("code", "ADMIN_AUTHENTICATION_REQUIRED");
                                error.put("message", "Administrator authentication is required.");
                                error.put("traceId", UUID.randomUUID().toString());
                                error.put("fieldErrors", Map.of());
                        } else if (request.getRequestURI().startsWith("/api/arcade/")) {
                                error.put("status", 401);
                                error.put("code", "ARCADE_AUTHENTICATION_REQUIRED");
                                error.put("message", "User authentication is required.");
                                error.put("traceId", UUID.randomUUID().toString());
                                error.put("fieldErrors", Map.of());
                        } else {
                                error.put("error", "인증이 필요합니다. 유효한 토큰을 제공해주세요.");
                                error.put("message", "Authentication required");
                        }

                        ObjectMapper objectMapper = new ObjectMapper();
                        objectMapper.writeValue(response.getWriter(), error);
                };
        }

        @Bean
        public AccessDeniedHandler accessDeniedHandler() {
                return (request, response, denied) -> {
                        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                        response.setCharacterEncoding("UTF-8");
                        Map<String, Object> error = new HashMap<>();
                        error.put("status", 403);
                        if (request.getRequestURI().startsWith("/api/arcade/")) {
                                error.put("code", "ARCADE_ACCESS_DENIED");
                                error.put("message", "User permission is required.");
                        } else {
                                error.put("code", "ADMIN_ACCESS_DENIED");
                                error.put("message", "Administrator permission is required.");
                        }
                        error.put("traceId", UUID.randomUUID().toString());
                        error.put("fieldErrors", Map.of());
                        new ObjectMapper().writeValue(response.getWriter(), error);
                };
        }

        @Bean
        public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
                http
                                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                                .csrf(csrf -> csrf.disable())
                                .sessionManagement(session -> session
                                                .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                                .authorizeHttpRequests(auth -> auth
                                                // OPTIONS 요청은 CORS preflight를 위해 인증 없이 허용
                                                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                                                .requestMatchers("/error").permitAll()
                                                // NextAuth server-to-server Google ID token exchange
                                                .requestMatchers(HttpMethod.POST, "/api/auth/google/exchange").permitAll()
                                                // Server-to-server crawler import; a pre-auth filter verifies its dedicated key.
                                                .requestMatchers(HttpMethod.POST, "/api/admin/articles/import").permitAll()
                                                // 회원가입/로그인 엔드포인트
                                                .requestMatchers("/api/users/signup", "/api/users/login").permitAll()
                                                // 어드민 로그인 엔드포인트
                                                .requestMatchers("/api/admin/login").permitAll()
                                                // 이메일 중복 확인
                                                .requestMatchers("/api/users/check-email").permitAll()
                                                // 공개 엔드포인트
                                                .requestMatchers("/api/articles/**").permitAll()
                                                .requestMatchers("/api/player").permitAll()
                                                .requestMatchers("/api/playerstat").permitAll()
                                                .requestMatchers("/api/playerstat/all").permitAll()
                                                .requestMatchers("/api/games/**").permitAll()
                                                .requestMatchers("/api/stadiums/**").permitAll()
                                                .requestMatchers("/api/users/me").hasRole("USER")
                                                // {userId} 패턴보다 먼저 둬야 한다. 뒤에 두면 permitAll에 먼저 걸려
                                                // 인증 없이 닉네임 사용 여부를 조회할 수 있다.
                                                .requestMatchers("/api/users/check-nickname").hasRole("USER")
                                                .requestMatchers("/api/users/{userId}").permitAll()
                                                .requestMatchers("/api/events/**").permitAll()
                                                .requestMatchers("/api/schedules/**").permitAll() // 스케줄 조회는 공개
                                                .requestMatchers(HttpMethod.GET, "/api/arcade/ranking").permitAll() // 랭킹 조회는 공개
                                                // 인증 필요 엔드포인트
                                                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                                                .requestMatchers("/api/diary/**").hasRole("USER")
                                                .requestMatchers("/api/arcade/**").hasRole("USER")
                                                .requestMatchers("/api/images/**").hasRole("USER")
                                                .anyRequest().authenticated())
                                .exceptionHandling(exception -> exception
                                                .authenticationEntryPoint(authenticationEntryPoint())
                                                .accessDeniedHandler(accessDeniedHandler()))
                                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                                .addFilterBefore(articleImportKeyAuthenticationFilter, JwtAuthenticationFilter.class);

                return http.build();
        }
}
