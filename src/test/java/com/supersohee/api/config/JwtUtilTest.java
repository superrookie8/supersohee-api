package com.supersohee.api.config;

import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtUtilTest {

    private static final String TEST_SECRET = "a-test-secret-that-is-at-least-thirty-two-bytes-long";

    @Test
    void userAndAdminTokensHaveSeparateSecurityBoundaries() {
        JwtUtil jwtUtil = jwtUtil(new MockEnvironment());

        JwtUtil.JwtPrincipal user = jwtUtil.parseAndValidateToken(jwtUtil.generateUserToken("user-1"));
        JwtUtil.JwtPrincipal admin = jwtUtil.parseAndValidateToken(jwtUtil.generateAdminToken("admin"));

        assertThat(user.subject()).isEqualTo("user-1");
        assertThat(user.role()).isEqualTo(JwtUtil.ROLE_USER);
        assertThat(user.tokenType()).isEqualTo(JwtUtil.USER_ACCESS_TOKEN);
        assertThat(admin.subject()).isEqualTo("admin");
        assertThat(admin.role()).isEqualTo(JwtUtil.ROLE_ADMIN);
        assertThat(admin.tokenType()).isEqualTo(JwtUtil.ADMIN_ACCESS_TOKEN);
    }

    @Test
    void rejectsTokenIssuedForAnotherApplication() {
        JwtUtil issuerA = jwtUtil(new MockEnvironment());
        JwtUtil issuerB = new JwtUtil(
                TEST_SECRET,
                60_000,
                "another-issuer",
                "web-audience",
                "admin-audience",
                new MockEnvironment());

        assertThatThrownBy(() -> issuerB.parseAndValidateToken(issuerA.generateUserToken("user-1")))
                .isInstanceOf(JwtException.class);
    }

    @Test
    void failsFastForShortSecret() {
        assertThatThrownBy(() -> new JwtUtil(
                "too-short",
                60_000,
                "issuer",
                "web-audience",
                "admin-audience",
                new MockEnvironment()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("at least 32 bytes");
    }

    @Test
    void forbidsKnownLegacySecretInProduction() {
        MockEnvironment production = new MockEnvironment();
        production.setActiveProfiles("prod");

        assertThatThrownBy(() -> new JwtUtil(
                "your-secret-key-must-be-at-least-256-bits-long-for-hs256-algorithm",
                60_000,
                "issuer",
                "web-audience",
                "admin-audience",
                production))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("forbidden");
    }

    private JwtUtil jwtUtil(MockEnvironment environment) {
        return new JwtUtil(
                TEST_SECRET,
                60_000,
                "issuer",
                "web-audience",
                "admin-audience",
                environment);
    }
}
