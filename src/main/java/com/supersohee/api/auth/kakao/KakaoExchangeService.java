package com.supersohee.api.auth.kakao;

import com.supersohee.api.auth.google.GoogleExchangeProtection;
import com.supersohee.api.config.JwtUtil;
import com.supersohee.api.user.domain.User;
import com.supersohee.api.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Service;

/**
 * Google exchange와 같은 계약이다. 교환 봉투(공유키·멱등성·재사용·레이트리밋)는
 * provider와 무관하므로 GoogleExchangeProtection을 그대로 재사용한다.
 * 이름이 Google로 남아 있는 건 네이버 추가 시 공용 패키지로 뽑을 때 함께 정리한다.
 */
@Service
@RequiredArgsConstructor
@ConditionalOnExpression("!'${kakao.auth.client-id:}'.trim().isEmpty()")
public class KakaoExchangeService {

    private static final String KAKAO_PROVIDER = "kakao";

    private final KakaoIdTokenVerifier idTokenVerifier;
    private final GoogleExchangeProtection exchangeProtection;
    private final UserService userService;
    private final JwtUtil jwtUtil;

    public KakaoExchangeResponse exchange(
            String exchangeKey,
            String idempotencyKey,
            String idToken) {
        return exchangeProtection.execute(
                exchangeKey,
                idempotencyKey,
                idToken,
                () -> performExchange(idToken));
    }

    private KakaoExchangeResponse performExchange(String idToken) {
        KakaoIdentity identity = idTokenVerifier.verify(idToken);
        User user = userService.findOrCreateUser(
                KAKAO_PROVIDER,
                identity.subject(),
                identity.email(),
                identity.name(),
                identity.picture());
        return new KakaoExchangeResponse(jwtUtil.generateUserToken(user.getId()), user.getId());
    }
}
