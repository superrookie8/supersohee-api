package com.supersohee.api.user.domain;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import com.supersohee.api.common.BaseDocument;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Document(collection = "users")
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

    // 업데이트 메서드
    public User updateProfile(String email, String nickname, String profileImageUrl) {
        return User.builder()
                .id(this.id)
                .provider(this.provider)
                .providerId(this.providerId) // 기존 값 유지
                .email(email)
                .nickname(nickname)
                .profileImageUrl(profileImageUrl != null ? profileImageUrl : this.profileImageUrl)
                .password(this.password)
                .points(this.points)
                .level(this.level)
                .build();
    }
}