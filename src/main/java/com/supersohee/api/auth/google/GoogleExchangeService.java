package com.supersohee.api.auth.google;

import com.supersohee.api.config.JwtUtil;
import com.supersohee.api.user.domain.User;
import com.supersohee.api.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GoogleExchangeService {

    private static final String GOOGLE_PROVIDER = "google";

    private final GoogleIdTokenVerifier idTokenVerifier;
    private final GoogleExchangeProtection exchangeProtection;
    private final UserService userService;
    private final JwtUtil jwtUtil;

    public GoogleExchangeResponse exchange(
            String exchangeKey,
            String idempotencyKey,
            String idToken) {
        return exchangeProtection.execute(
                exchangeKey,
                idempotencyKey,
                idToken,
                () -> performExchange(idToken));
    }

    private GoogleExchangeResponse performExchange(String idToken) {
        GoogleIdentity identity = idTokenVerifier.verify(idToken);
        User user = userService.findOrCreateUser(
                GOOGLE_PROVIDER,
                identity.subject(),
                identity.email(),
                identity.name(),
                identity.picture());
        return new GoogleExchangeResponse(jwtUtil.generateUserToken(user.getId()), user.getId());
    }
}
