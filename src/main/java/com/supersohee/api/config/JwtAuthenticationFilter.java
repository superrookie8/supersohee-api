package com.supersohee.api.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    public static final String TOKEN_ISSUED_AT_ATTRIBUTE = "supersohee.auth.tokenIssuedAt";

    private final JwtUtil jwtUtil;
    private final com.supersohee.api.user.repository.UserRepository userRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        if ("OPTIONS".equals(request.getMethod())) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = getTokenFromRequest(request);
        if (token != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            try {
                JwtUtil.JwtPrincipal principal = jwtUtil.parseAndValidateToken(token);
                if (!JwtUtil.ROLE_USER.equals(principal.role())) {
                    throw new io.jsonwebtoken.JwtException("Legacy administrator tokens are disabled");
                }
                var user = userRepository.findById(principal.subject())
                        .orElseThrow(() -> new io.jsonwebtoken.JwtException("Token subject no longer exists"));
                var authorities = new java.util.ArrayList<SimpleGrantedAuthority>();
                authorities.add(new SimpleGrantedAuthority(principal.role()));
                // Social/member JWTs remain USER tokens. Check the server-owned DB
                // permission afresh on every admin request, so revocation needs no logout.
                if (JwtUtil.ROLE_USER.equals(principal.role()) && request.getRequestURI().startsWith("/api/admin/")) {
                    if ("ADMIN".equals(user.getRole())) {
                        authorities.add(new SimpleGrantedAuthority(JwtUtil.ROLE_ADMIN));
                    }
                }
                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                        principal.subject(),
                        null,
                        authorities);
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authentication);
                request.setAttribute(TOKEN_ISSUED_AT_ATTRIBUTE, principal.issuedAt());
            } catch (RuntimeException ignored) {
                SecurityContextHolder.clearContext();
            }
        }

        filterChain.doFilter(request, response);
    }

    private String getTokenFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}
