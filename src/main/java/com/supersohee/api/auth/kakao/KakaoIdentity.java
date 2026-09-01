package com.supersohee.api.auth.kakao;

/**
 * 카카오 ID 토큰에서 검증을 마친 사용자 신원.
 *
 * email은 없을 수 있다. 카카오 이메일 동의항목은 비즈 앱 심사를 통과해야 쓸 수 있고,
 * 승인 전에는 claim 자체가 내려오지 않는다. PRD §7.4.1 참고.
 */
public record KakaoIdentity(
        String subject,
        String email,
        String name,
        String picture) {
}
