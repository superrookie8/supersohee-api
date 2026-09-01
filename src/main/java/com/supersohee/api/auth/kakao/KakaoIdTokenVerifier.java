package com.supersohee.api.auth.kakao;

public interface KakaoIdTokenVerifier {
    KakaoIdentity verify(String idToken);
}
