package com.supersohee.api.user.domain;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;
import com.supersohee.api.common.BaseDocument;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Document(collection = "users")
@CompoundIndex(
        name = "provider_subject_unique",
        def = "{'provider': 1, 'providerId': 1}",
        unique = true,
        partialFilter = "{'provider': {$type: 'string'}, 'providerId': {$type: 'string'}}")
// 닉네임은 방명록·랭킹에서 사람을 식별하는 값이므로 중복을 허용하지 않는다.
// 닉네임이 없는 과거 문서는 partialFilter로 제외한다.
@CompoundIndex(
        name = "nickname_unique",
        def = "{'nickname': 1}",
        unique = true,
        partialFilter = "{'nickname': {$type: 'string'}}")
public class User extends BaseDocument {
    @Id
    private String id;

    // OAuth
    private String provider; // google
    private String providerId; // google sub (unique)

    private String email;
    private String nickname;
    private String profileImageUrl;

    // 일반 회원가입용 비밀번호 (암호화된 상태로 저장)
    private String password;

    // 활동 요약 (선택, 나중에 추가 가능)
    private Integer points;
    private Integer level;

    // createdAt, updatedAt은 BaseDocument에서 상속받음

    private User copy(String email, String nickname, String profileImageUrl) {
        return User.builder()
                .id(this.id)
                .provider(this.provider)
                .providerId(this.providerId) // 기존 값 유지
                .email(email)
                .nickname(nickname)
                .profileImageUrl(profileImageUrl)
                .password(this.password)
                .points(this.points)
                .level(this.level)
                .build();
    }

    /**
     * 로그인할 때 provider가 준 정보를 반영한다.
     *
     * 닉네임과 프로필 사진은 사용자가 마이페이지에서 직접 정하는 값이므로 이미 값이
     * 있으면 provider 값으로 덮어쓰지 않는다. 예전 구현은 매 로그인마다 닉네임을
     * Google 계정 이름으로 되돌려, 사용자가 바꾼 닉네임이 유지되지 않았다.
     */
    public User syncProviderProfile(String email, String providerNickname, String providerImageUrl) {
        return copy(
                email != null ? email : this.email,
                // provider 표시이름은 대개 본명이라 닉네임에 쓰지 않는다.
                // 닉네임은 가입 시 생성된 값이거나 사용자가 직접 정한 값만 갖는다.
                this.nickname,
                hasText(this.profileImageUrl) ? this.profileImageUrl : providerImageUrl);
    }

    /**
     * 사용자가 마이페이지에서 직접 수정한 값을 반영한다.
     * null은 "변경하지 않음"을 뜻하고, 프로필 사진은 빈 문자열로 삭제할 수 있다.
     */
    public User withProfileEdits(String nickname, String profileImageUrl) {
        return copy(
                this.email,
                hasText(nickname) ? nickname.trim() : this.nickname,
                profileImageUrl == null
                        ? this.profileImageUrl
                        : (profileImageUrl.isBlank() ? null : profileImageUrl.trim()));
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
