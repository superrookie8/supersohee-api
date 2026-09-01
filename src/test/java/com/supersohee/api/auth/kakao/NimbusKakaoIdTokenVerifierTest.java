package com.supersohee.api.auth.kakao;

import io.jsonwebtoken.Jwts;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPublicKey;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class NimbusKakaoIdTokenVerifierTest {

    private static final String CLIENT_ID = "kakao-rest-api-key";

    private KeyPair signingKeys;
    private Instant now;
    private NimbusKakaoIdTokenVerifier verifier;

    @BeforeEach
    void setUp() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        signingKeys = generator.generateKeyPair();
        now = Instant.now();
        verifier = new NimbusKakaoIdTokenVerifier(
                NimbusJwtDecoder.withPublicKey((RSAPublicKey) signingKeys.getPublic()).build(),
                CLIENT_ID,
                Clock.fixed(now, ZoneOffset.UTC),
                Duration.ofSeconds(30),
                16_384);
    }

    @Test
    void verifiesSignatureIssuerAudienceTimesAndSubject() {
        KakaoIdentity identity = verifier.verify(token(
                signingKeys, NimbusKakaoIdTokenVerifier.KAKAO_ISSUER, CLIENT_ID,
                now.minusSeconds(5), now.plusSeconds(300), "kakao-subject",
                Map.of("nickname", "소히팬", "picture", "https://image.test/p.jpg")));

        assertThat(identity.subject()).isEqualTo("kakao-subject");
        assertThat(identity.name()).isEqualTo("소히팬");
        assertThat(identity.picture()).isEqualTo("https://image.test/p.jpg");
    }

    /** 이메일 동의항목은 비즈 앱 심사 전까지 내려오지 않는다. 그래도 로그인은 되어야 한다. */
    @Test
    void acceptsTokenWithoutEmail() {
        KakaoIdentity identity = verifier.verify(token(
                signingKeys, NimbusKakaoIdTokenVerifier.KAKAO_ISSUER, CLIENT_ID,
                now.minusSeconds(5), now.plusSeconds(300), "kakao-subject", Map.of()));

        assertThat(identity.subject()).isEqualTo("kakao-subject");
        assertThat(identity.email()).isNull();
    }

    /** 카카오는 email_verified를 주지 않으므로 동의로 내려온 이메일은 그대로 쓴다. */
    @Test
    void keepsEmailWhenPresent() {
        KakaoIdentity identity = verifier.verify(token(
                signingKeys, NimbusKakaoIdTokenVerifier.KAKAO_ISSUER, CLIENT_ID,
                now.minusSeconds(5), now.plusSeconds(300), "kakao-subject",
                Map.of("email", "fan@example.test")));

        assertThat(identity.email()).isEqualTo("fan@example.test");
    }

    @Test
    void rejectsInvalidSignature() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        KeyPair attackerKeys = generator.generateKeyPair();

        assertInvalid(token(attackerKeys, NimbusKakaoIdTokenVerifier.KAKAO_ISSUER, CLIENT_ID,
                now.minusSeconds(5), now.plusSeconds(300), "kakao-subject", Map.of()));
    }

    /** Google 토큰을 카카오 엔드포인트로 밀어넣는 provider 혼동 공격을 막는다. */
    @Test
    void rejectsUnexpectedIssuer() {
        assertInvalid(token(signingKeys, "https://accounts.google.com", CLIENT_ID,
                now.minusSeconds(5), now.plusSeconds(300), "kakao-subject", Map.of()));
    }

    @Test
    void rejectsUnexpectedAudience() {
        assertInvalid(token(signingKeys, NimbusKakaoIdTokenVerifier.KAKAO_ISSUER, "another-app",
                now.minusSeconds(5), now.plusSeconds(300), "kakao-subject", Map.of()));
    }

    @Test
    void rejectsExpiredToken() {
        assertInvalid(token(signingKeys, NimbusKakaoIdTokenVerifier.KAKAO_ISSUER, CLIENT_ID,
                now.minusSeconds(300), now.minusSeconds(60), "kakao-subject", Map.of()));
    }

    @Test
    void rejectsFutureIssuedAt() {
        assertInvalid(token(signingKeys, NimbusKakaoIdTokenVerifier.KAKAO_ISSUER, CLIENT_ID,
                now.plusSeconds(120), now.plusSeconds(300), "kakao-subject", Map.of()));
    }

    @Test
    void rejectsBlankToken() {
        assertThatThrownBy(() -> verifier.verify("  "))
                .isInstanceOf(InvalidKakaoTokenException.class);
    }

    private void assertInvalid(String token) {
        assertThatThrownBy(() -> verifier.verify(token))
                .isInstanceOf(InvalidKakaoTokenException.class);
    }

    private String token(
            KeyPair keys,
            String issuer,
            String audience,
            Instant issuedAt,
            Instant expiresAt,
            String subject,
            Map<String, Object> extraClaims) {
        var builder = Jwts.builder()
                .issuer(issuer)
                .audience().add(List.of(audience)).and()
                .issuedAt(Date.from(issuedAt))
                .expiration(Date.from(expiresAt))
                .subject(subject);
        extraClaims.forEach(builder::claim);
        return builder.signWith(keys.getPrivate(), Jwts.SIG.RS256).compact();
    }
}
