package com.supersohee.api.config;

import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfigurationSource;

import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

        private final JwtAuthenticationFilter jwtAuthenticationFilter;
        private final OAuth2SuccessHandler oAuth2SuccessHandler;
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

                        Map<String, String> error = new HashMap<>();
                        error.put("error", "인증이 필요합니다. 유효한 토큰을 제공해주세요.");
                        error.put("message", "Authentication required");

                        ObjectMapper objectMapper = new ObjectMapper();
                        objectMapper.writeValue(response.getWriter(), error);
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
                                                // OAuth2 로그인 엔드포인트
                                                .requestMatchers("/oauth2/**", "/login/oauth2/**").permitAll()
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
                                                .requestMatchers("/api/users/{userId}").permitAll()
                                                .requestMatchers("/api/events/**").permitAll()
                                                .requestMatchers("/api/schedules/**").permitAll() // 스케줄 조회는 공개
                                                // 인증 필요 엔드포인트
                                                .requestMatchers("/api/users/me").authenticated()
                                                .requestMatchers("/api/diary/**").authenticated()
                                                .requestMatchers("/api/arcade/**").authenticated()
                                                .requestMatchers("/api/images/**").authenticated()
                                                .requestMatchers("/api/admin/**").authenticated() // 어드민 API는 인증 필요
                                                .anyRequest().authenticated())
                                .oauth2Login(oauth2 -> oauth2
                                                .successHandler(oAuth2SuccessHandler))
                                .exceptionHandling(exception -> exception
                                                .authenticationEntryPoint(authenticationEntryPoint())) // 인증 실패 핸들러 추가
                                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

                return http.build();
        }
}