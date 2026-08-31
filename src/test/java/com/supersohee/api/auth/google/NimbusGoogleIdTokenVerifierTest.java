package com.supersohee.api.auth.google;

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
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class NimbusGoogleIdTokenVerifierTest {

    private static final String CLIENT_ID = "google-client-id";

    private KeyPair signingKeys;
    private Instant now;
    private NimbusGoogleIdTokenVerifier verifier;

    @BeforeEach
    void setUp() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        signingKeys = generator.generateKeyPair();
        now = Instant.now();
        verifier = verifierFor((RSAPublicKey) signingKeys.getPublic());
    }

    @Test
    void verifiesSignatureIssuerAudienceTimesSubjectAndVerifiedEmail() {
        GoogleIdentity identity = verifier.verify(token(
                signingKeys,
                NimbusGoogleIdTokenVerifier.MODERN_GOOGLE_ISSUER,
                CLIENT_ID,
                now.minusSeconds(5),
                now.plusSeconds(300),
                "google-subject",
                true));

        assertThat(identity.subject()).isEqualTo("google-subject");
        assertThat(identity.email()).isEqualTo("fan@example.test");
    }

    @Test
    void rejectsInvalidSignature() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        KeyPair attackerKeys = generator.generateKeyPair();

        assertInvalid(token(
                attackerKeys,
                NimbusGoogleIdTokenVerifier.MODERN_GOOGLE_ISSUER,
                CLIENT_ID,
                now.minusSeconds(5),
                now.plusSeconds(300),
                "google-subject",
                true));
    }

    @Test
    void rejectsUnexpectedIssuer() {
        assertInvalid(token(
                signingKeys,
                "https://issuer.invalid",
                CLIENT_ID,
                now.minusSeconds(5),
                now.plusSeconds(300),
                "google-subject",
                true));
    }

    @Test
    void rejectsUnexpectedAudience() {
        assertInvalid(token(
                signingKeys,
                NimbusGoogleIdTokenVerifier.MODERN_GOOGLE_ISSUER,
                "another-client",
                now.minusSeconds(5),
                now.plusSeconds(300),
                "google-subject",
                true));
    }

    @Test
    void rejectsExpiredToken() {
        assertInvalid(token(
                signingKeys,
                NimbusGoogleIdTokenVerifier.MODERN_GOOGLE_ISSUER,
                CLIENT_ID,
                now.minusSeconds(300),
                now.minusSeconds(60),
                "google-subject",
                true));
    }

    @Test
    void rejectsFutureIssuedAt() {
        assertInvalid(token(
                signingKeys,
                NimbusGoogleIdTokenVerifier.MODERN_GOOGLE_ISSUER,
                CLIENT_ID,
                now.plusSeconds(120),
                now.plusSeconds(300),
                "google-subject",
                true));
    }

    @Test
    void acceptsUnverifiedEmailWithoutPersistableEmail() {
        GoogleIdentity identity = verifier.verify(token(
                signingKeys,
                NimbusGoogleIdTokenVerifier.MODERN_GOOGLE_ISSUER,
                CLIENT_ID,
                now.minusSeconds(5),
                now.plusSeconds(300),
                "google-subject",
                false));

        assertThat(identity.subject()).isEqualTo("google-subject");
        assertThat(identity.email()).isNull();
    }

    @Test
    void acceptsMissingEmailWithoutPersistableEmail() {
        GoogleIdentity identity = verifier.verify(token(
                signingKeys,
                NimbusGoogleIdTokenVerifier.MODERN_GOOGLE_ISSUER,
                List.of(CLIENT_ID),
                now.minusSeconds(5),
                now.plusSeconds(300),
                "google-subject",
                null,
                false,
                null));

        assertThat(identity.subject()).isEqualTo("google-subject");
        assertThat(identity.email()).isNull();
    }

    @Test
    void requiresMatchingAuthorizedPartyForMultipleAudiences() {
        String token = token(
                signingKeys,
                NimbusGoogleIdTokenVerifier.MODERN_GOOGLE_ISSUER,
                List.of(CLIENT_ID, "another-client"),
                now.minusSeconds(5),
                now.plusSeconds(300),
                "google-subject",
                true,
                true,
                "another-client");

        assertInvalid(token);
    }

    @Test
    void acceptsMultipleAudiencesWhenAuthorizedPartyMatches() {
        GoogleIdentity identity = verifier.verify(token(
                signingKeys,
                NimbusGoogleIdTokenVerifier.MODERN_GOOGLE_ISSUER,
                List.of(CLIENT_ID, "another-client"),
                now.minusSeconds(5),
                now.plusSeconds(300),
                "google-subject",
                true,
                true,
                CLIENT_ID));

        assertThat(identity.subject()).isEqualTo("google-subject");
    }

    private NimbusGoogleIdTokenVerifier verifierFor(RSAPublicKey publicKey) {
        return new NimbusGoogleIdTokenVerifier(
                NimbusJwtDecoder.withPublicKey(publicKey).build(),
                CLIENT_ID,
                Set.of(
                        NimbusGoogleIdTokenVerifier.MODERN_GOOGLE_ISSUER,
                        NimbusGoogleIdTokenVerifier.LEGACY_GOOGLE_ISSUER),
                Clock.fixed(now, ZoneOffset.UTC),
                Duration.ofSeconds(30),
                16_384);
    }

    private String token(
            KeyPair keys,
            String issuer,
            String audience,
            Instant issuedAt,
            Instant expiresAt,
            String subject,
            boolean emailVerified) {
        return token(
                keys,
                issuer,
                List.of(audience),
                issuedAt,
                expiresAt,
                subject,
                emailVerified,
                true,
                null);
    }

    private String token(
            KeyPair keys,
            String issuer,
            List<String> audiences,
            Instant issuedAt,
            Instant expiresAt,
            String subject,
            Boolean emailVerified,
            boolean includeEmail,
            String authorizedParty) {
        var builder = Jwts.builder()
                .issuer(issuer)
                .subject(subject)
                .claim("aud", audiences.size() == 1 ? audiences.get(0) : audiences)
                .claim("name", "Fan")
                .issuedAt(Date.from(issuedAt))
                .expiration(Date.from(expiresAt));
        if (includeEmail) {
            builder.claim("email", "fan@example.test");
        }
        if (emailVerified != null) {
            builder.claim("email_verified", emailVerified);
        }
        if (authorizedParty != null) {
            builder.claim("azp", authorizedParty);
        }
        return builder
                .signWith(keys.getPrivate(), Jwts.SIG.RS256)
                .compact();
    }

    private void assertInvalid(String token) {
        assertThatThrownBy(() -> verifier.verify(token))
                .isInstanceOf(InvalidGoogleTokenException.class);
    }
}
