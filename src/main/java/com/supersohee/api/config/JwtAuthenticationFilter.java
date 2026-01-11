package com.supersohee.api.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        // OPTIONS 요청은 CORS preflight이므로 필터 건너뛰기
        if ("OPTIONS".equals(request.getMethod())) {
            System.out.println("=== OPTIONS Request (CORS Preflight) ===");
            System.out.println("Request URI: " + request.getRequestURI());
            System.out.println("Origin: " + request.getHeader("Origin"));
            System.out.println("Access-Control-Request-Method: " + request.getHeader("Access-Control-Request-Method"));
            System.out
                    .println("Access-Control-Request-Headers: " + request.getHeader("Access-Control-Request-Headers"));
            System.out.println("==========================================");
            filterChain.doFilter(request, response);
            return;
        }

        String requestURI = request.getRequestURI();

        // 공개 엔드포인트는 토큰 검증 없이 통과
        if (isPublicEndpoint(requestURI)) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = getTokenFromRequest(request);

        System.out.println("=== JWT Filter Debug ===");
        System.out.println("Request URI: " + requestURI);
        System.out.println(
                "Authorization header: " + (request.getHeader("Authorization") != null ? "present" : "missing"));

        if (token != null) {
            System.out.println("Token received: " + token.substring(0, Math.min(20, token.length())) + "...");
            boolean isValid = jwtUtil.validateToken(token);
            System.out.println("Token validation result: " + isValid);

            if (isValid) {
                String userId = jwtUtil.getUserIdFromToken(token);
                System.out.println("User ID extracted: " + userId);

                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(userId,
                        null,
                        new ArrayList<>());
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                SecurityContextHolder.getContext().setAuthentication(authentication);
                System.out.println("Authentication set successfully");
            } else {
                // 토큰이 유효하지 않을 때 로깅 (디버깅용)
                System.out
                        .println("Invalid token received: " + token.substring(0, Math.min(20, token.length())) + "...");
                jwtUtil.validateTokenWithDetails(token); // 상세 에러 로깅
            }
        } else {
            // 토큰이 없을 때 로깅 (디버깅용)
            System.out.println("No token found in request to: " + requestURI);
            System.out.println("All headers: " + java.util.Collections.list(request.getHeaderNames()));
        }
        System.out.println("========================");

        filterChain.doFilter(request, response);
    }

    private String getTokenFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }

    // 공개 엔드포인트 확인
    private boolean isPublicEndpoint(String requestURI) {
        return requestURI.startsWith("/api/users/signup") ||
                requestURI.startsWith("/api/users/login") ||
                requestURI.startsWith("/api/admin/login") ||
                requestURI.startsWith("/api/users/check-email") ||
                requestURI.startsWith("/api/articles") ||
                requestURI.equals("/api/player") ||
                requestURI.startsWith("/api/playerstat") ||
                requestURI.startsWith("/api/games") ||
                requestURI.startsWith("/api/stadiums") ||
                requestURI.startsWith("/api/events") ||
                requestURI.startsWith("/api/schedules") ||
                requestURI.startsWith("/oauth2") ||
                requestURI.startsWith("/login/oauth2");
    }
}