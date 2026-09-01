package com.supersohee.api.auth.kakao;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
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

/**
 * 카카오 ID 토큰 검증기. 값은 라이브 OIDC discovery에서 확인했다.
 * https://kauth.kakao.com/.well-known/openid-configuration
 *
 * Google 검증기와 형태가 같지만 두 가지가 다르다.
 * - 카카오는 email_verified claim을 주지 않는다. 이메일은 동의를 받은 경우에만 들어오며
 *   그 자체로 검증된 값으로 취급한다.
 * - sub가 pairwise(앱 단위)다. 다른 provider의 sub와 같은 사람인지 비교할 수 없다.
 *
 * KAKAO_CLIENT_ID가 없으면 이 빈을 만들지 않는다. 카카오를 설정하지 않은 환경에서
 * 애플리케이션이 뜨지 않거나 Google 로그인이 함께 죽으면 안 된다(PRD §7.4.1).
 */
@Component
@ConditionalOnExpression("!'${kakao.auth.client-id:}'.trim().isEmpty()")
public class NimbusKakaoIdTokenVerifier implements KakaoIdTokenVerifier {

    static final String KAKAO_ISSUER = "https://kauth.kakao.com";
    static final String KAKAO_JWKS_URI = "https://kauth.kakao.com/.well-known/jwks.json";

    private final JwtDecoder decoder;
    private final String clientId;
    private final Clock clock;
    private final Duration clockSkew;
    private final int maximumTokenLength;

    @Autowired
    public NimbusKakaoIdTokenVerifier(
            @Value("${kakao.auth.client-id:}") String clientId,
            @Value("${kakao.auth.jwks-uri:" + KAKAO_JWKS_URI + "}") String jwksUri,
            @Value("${kakao.auth.clock-skew-seconds:30}") long clockSkewSeconds,
            @Value("${kakao.auth.maximum-token-length:16384}") int maximumTokenLength) {
        this(
                buildDecoder(jwksUri),
                clientId,
                Clock.systemUTC(),
                Duration.ofSeconds(clockSkewSeconds),
                maximumTokenLength);
    }

    NimbusKakaoIdTokenVerifier(
            JwtDecoder decoder,
            String clientId,
            Clock clock,
            Duration clockSkew,
            int maximumTokenLength) {
        this.decoder = decoder;
        this.clientId = requireText(clientId, "kakao.auth.client-id must be configured");
        this.clock = clock;
        if (clockSkew.isNegative()) {
            throw new IllegalStateException("kakao.auth.clock-skew-seconds must not be negative");
        }
        this.clockSkew = clockSkew;
        if (maximumTokenLength < 1) {
            throw new IllegalStateException("kakao.auth.maximum-token-length must be greater than zero");
        }
        this.maximumTokenLength = maximumTokenLength;
    }

    @Override
    public KakaoIdentity verify(String idToken) {
        if (idToken == null || idToken.isBlank() || idToken.length() > maximumTokenLength) {
            throw new InvalidKakaoTokenException();
        }

        final Jwt jwt;
        try {
            jwt = decoder.decode(idToken);
        } catch (JwtException | IllegalArgumentException e) {
            throw new InvalidKakaoTokenException(e);
        }

        Instant now = clock.instant();
        Instant expiresAt = jwt.getExpiresAt();
        Instant issuedAt = jwt.getIssuedAt();
        String subject = jwt.getSubject();
        List<String> audience = jwt.getAudience();

        if (!KAKAO_ISSUER.equals(jwt.getClaimAsString("iss"))
                || audience == null || audience.size() != 1 || !audience.contains(clientId)
                || expiresAt == null || !expiresAt.isAfter(now.minus(clockSkew))
                || issuedAt == null || issuedAt.isAfter(now.plus(clockSkew))
                || subject == null || subject.isBlank() || subject.length() > 255) {
            throw new InvalidKakaoTokenException();
        }

        String email = jwt.getClaimAsString("email");
        return new KakaoIdentity(
                subject,
                email != null && !email.isBlank() ? email : null,
                jwt.getClaimAsString("nickname"),
                jwt.getClaimAsString("picture"));
    }

    private static JwtDecoder buildDecoder(String jwksUri) {
        String validatedUri = requireText(jwksUri, "kakao.auth.jwks-uri must be configured");
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
}
