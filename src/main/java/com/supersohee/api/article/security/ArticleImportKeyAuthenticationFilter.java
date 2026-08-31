package com.supersohee.api.article.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.supersohee.api.admin.error.AdminApiException;
import com.supersohee.api.admin.error.AdminErrorResponse;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class ArticleImportKeyAuthenticationFilter extends OncePerRequestFilter {
    static final String HEADER_NAME = "X-Article-Import-Key";
    private static final String IMPORT_PATH = "/api/admin/articles/import";

    private final ArticleImportKeyGuard importKeyGuard;
    private final ObjectMapper objectMapper;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String requestPath = request.getRequestURI();
        String contextPath = request.getContextPath();
        if (!contextPath.isEmpty() && requestPath.startsWith(contextPath)) {
            requestPath = requestPath.substring(contextPath.length());
        }
        return !"POST".equals(request.getMethod()) || !IMPORT_PATH.equals(requestPath);
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        try {
            importKeyGuard.verify(request.getHeader(HEADER_NAME));
            filterChain.doFilter(request, response);
        } catch (AdminApiException exception) {
            response.setStatus(exception.status().value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding("UTF-8");
            objectMapper.writeValue(response.getWriter(), new AdminErrorResponse(
                    exception.status().value(),
                    exception.code(),
                    exception.safeMessage(),
                    UUID.randomUUID().toString(),
                    Map.of()));
        }
    }
}
