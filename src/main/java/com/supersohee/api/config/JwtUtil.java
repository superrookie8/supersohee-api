package com.supersohee.api.config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Objects;
import java.util.Set;

@Component
public class JwtUtil {

    public static final String ROLE_USER = "ROLE_USER";
    public static final String ROLE_ADMIN = "ROLE_ADMIN";
    public static final String USER_ACCESS_TOKEN = "USER_ACCESS";
    public static final String ADMIN_ACCESS_TOKEN = "ADMIN_ACCESS";

    private static final String LEGACY_DEFAULT_SECRET =
            "your-secret-key-must-be-at-least-256-bits-long-for-hs256-algorithm";
    private static final int MINIMUM_SECRET_BYTES = 32;

    private final long expiration;
    private final String issuer;
    private final String userAudience;
    private final String adminAudience;
    private final SecretKey signingKey;

    public JwtUtil(
            @Value("${jwt.secret:}") String secret,
            @Value("${jwt.expiration:86400000}") long expiration,
            @Value("${jwt.issuer:supersohee-api}") String issuer,
            @Value("${jwt.audience.user:supersohee-web}") String userAudience,
            @Value("${jwt.audience.admin:supersohee-admin}") String adminAudience,
            Environment environment) {
        String validatedSecret = requireText(secret, "jwt.secret must be configured");
        this.expiration = requirePositive(expiration, "jwt.expiration must be greater than zero");
        this.issuer = requireText(issuer, "jwt.issuer must be configured");
        this.userAudience = requireText(userAudience, "jwt.audience.user must be configured");
        this.adminAudience = requireText(adminAudience, "jwt.audience.admin must be configured");

        byte[] secretBytes = validatedSecret.getBytes(StandardCharsets.UTF_8);
        if (secretBytes.length < MINIMUM_SECRET_BYTES) {
            throw new IllegalStateException("jwt.secret must be at least 32 bytes for HS256");
        }
        if (environment.acceptsProfiles(Profiles.of("prod"))
                && LEGACY_DEFAULT_SECRET.equals(validatedSecret)) {
            throw new IllegalStateException("The known legacy jwt.secret is forbidden in the prod profile");
        }
        this.signingKey = Keys.hmacShaKeyFor(secretBytes);
    }

    public String generateUserToken(String userId) {
        return generateToken(userId, ROLE_USER, USER_ACCESS_TOKEN, userAudience);
    }

    public String generateAdminToken(String adminId) {
        return generateToken(adminId, ROLE_ADMIN, ADMIN_ACCESS_TOKEN, adminAudience);
    }

    public JwtPrincipal parseAndValidateToken(String token) {
        Claims claims = parseClaims(token);
        String subject = requireClaim(claims, Claims.SUBJECT);
        String tokenIssuer = requireClaim(claims, Claims.ISSUER);
        Set<String> audiences = claims.getAudience();
        String role = requireClaim(claims, "role");
        String tokenType = requireClaim(claims, "token_type");

        if (!issuer.equals(tokenIssuer)) {
            throw new JwtException("Unexpected token issuer");
        }

        String expectedAudience;
        String expectedTokenType;
        if (ROLE_ADMIN.equals(role)) {
            expectedAudience = adminAudience;
            expectedTokenType = ADMIN_ACCESS_TOKEN;
        } else if (ROLE_USER.equals(role)) {
            expectedAudience = userAudience;
            expectedTokenType = USER_ACCESS_TOKEN;
        } else {
            throw new JwtException("Unsupported token role");
        }

        if (audiences == null || audiences.size() != 1 || !audiences.contains(expectedAudience)) {
            throw new JwtException("Unexpected token audience");
        }
        if (!expectedTokenType.equals(tokenType)) {
            throw new JwtException("Unexpected token type");
        }

        return new JwtPrincipal(subject, role, tokenType);
    }

    public boolean validateToken(String token) {
        try {
            parseAndValidateToken(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    private String generateToken(String subject, String role, String tokenType, String audience) {
        String validatedSubject = requireText(subject, "JWT subject must not be blank");
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expiration);

        return Jwts.builder()
                .issuer(issuer)
                .subject(validatedSubject)
                .claim(Claims.AUDIENCE, audience)
                .claim("role", role)
                .claim("token_type", tokenType)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(signingKey)
                .compact();
    }

    private Claims parseClaims(String token) {
        if (token == null || token.isBlank()) {
            throw new IllegalArgumentException("JWT must not be blank");
        }
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private static String requireClaim(Claims claims, String name) {
        Object value = claims.get(name);
        if (!(value instanceof String text) || text.isBlank()) {
            throw new JwtException("Missing or invalid claim: " + name);
        }
        return text;
    }

    private static String requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(message);
        }
        return value;
    }

    private static long requirePositive(long value, String message) {
        if (value <= 0) {
            throw new IllegalStateException(message);
        }
        return value;
    }

    public record JwtPrincipal(String subject, String role, String tokenType) {
        public JwtPrincipal {
            Objects.requireNonNull(subject);
            Objects.requireNonNull(role);
            Objects.requireNonNull(tokenType);
        }
    }
}
