package com.supersohee.api.auth.google;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Set;

@Component
public class NimbusGoogleIdTokenVerifier implements GoogleIdTokenVerifier {

    static final String MODERN_GOOGLE_ISSUER = "https://accounts.google.com";
    static final String LEGACY_GOOGLE_ISSUER = "accounts.google.com";
    static final String GOOGLE_JWKS_URI = "https://www.googleapis.com/oauth2/v3/certs";

    private final JwtDecoder decoder;
    private final String clientId;
    private final Set<String> allowedIssuers;
    private final Clock clock;
    private final Duration clockSkew;
    private final int maximumTokenLength;

    @Autowired
    public NimbusGoogleIdTokenVerifier(
            @Value("${google.auth.client-id:${GOOGLE_CLIENT_ID:}}") String clientId,
            @Value("${google.auth.jwks-uri:" + GOOGLE_JWKS_URI + "}") String jwksUri,
            @Value("${google.auth.clock-skew-seconds:30}") long clockSkewSeconds,
            @Value("${google.auth.maximum-token-length:16384}") int maximumTokenLength) {
        this(
                buildDecoder(jwksUri),
                clientId,
                Set.of(MODERN_GOOGLE_ISSUER, LEGACY_GOOGLE_ISSUER),
                Clock.systemUTC(),
                Duration.ofSeconds(clockSkewSeconds),
                maximumTokenLength);
    }

    NimbusGoogleIdTokenVerifier(
            JwtDecoder decoder,
            String clientId,
            Set<String> allowedIssuers,
            Clock clock,
            Duration clockSkew,
            int maximumTokenLength) {
        this.decoder = decoder;
        this.clientId = requireText(clientId, "google.auth.client-id must be configured");
        this.allowedIssuers = Set.copyOf(allowedIssuers);
        this.clock = clock;
        this.clockSkew = requireNonNegative(clockSkew, "google.auth.clock-skew-seconds must not be negative");
        if (maximumTokenLength < 1) {
            throw new IllegalStateException("google.auth.maximum-token-length must be greater than zero");
        }
        this.maximumTokenLength = maximumTokenLength;
    }

    @Override
    public GoogleIdentity verify(String idToken) {
        if (idToken == null || idToken.isBlank() || idToken.length() > maximumTokenLength) {
            throw new InvalidGoogleTokenException();
        }

        final Jwt jwt;
        try {
            jwt = decoder.decode(idToken);
        } catch (JwtException | IllegalArgumentException e) {
            throw new InvalidGoogleTokenException(e);
        }

        Instant now = clock.instant();
        Instant expiresAt = jwt.getExpiresAt();
        Instant issuedAt = jwt.getIssuedAt();
        String subject = jwt.getSubject();
        String email = jwt.getClaimAsString("email");
        Boolean emailVerified = jwt.getClaim("email_verified");
        List<String> audience = jwt.getAudience();
        String authorizedParty = jwt.getClaimAsString("azp");

        if (!allowedIssuers.contains(jwt.getClaimAsString("iss"))
                || audience == null || !audience.contains(clientId)
                || audience.size() > 1 && !clientId.equals(authorizedParty)
                || expiresAt == null || !expiresAt.isAfter(now.minus(clockSkew))
                || issuedAt == null || issuedAt.isAfter(now.plus(clockSkew))
                || subject == null || subject.isBlank() || subject.length() > 255) {
            throw new InvalidGoogleTokenException();
        }

        String verifiedEmail = Boolean.TRUE.equals(emailVerified) && email != null && !email.isBlank()
                ? email
                : null;
        return new GoogleIdentity(
                subject,
                verifiedEmail,
                jwt.getClaimAsString("name"),
                jwt.getClaimAsString("picture"));
    }

    private static JwtDecoder buildDecoder(String jwksUri) {
        String validatedUri = requireText(jwksUri, "google.auth.jwks-uri must be configured");
        return NimbusJwtDecoder.withJwkSetUri(validatedUri)
                .jwsAlgorithm(SignatureAlgorithm.RS256)
                .build();
    }

    private static String requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(message);
        }
        return value;
    }

    private static Duration requireNonNegative(Duration value, String message) {
        if (value.isNegative()) {
            throw new IllegalStateException(message);
        }
        return value;
    }
}
