package com.supersohee.api.user.dto;

import com.supersohee.api.user.domain.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserResponse {
    private String id;
    private String provider;
    private String providerId;
    private String email;
    private String nickname;
    private String profileImageUrl;
    private Integer points;
    private Integer level;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // User 도메인에서 UserResponse로 변환하는 정적 팩토리 메서드
    public static UserResponse from(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .provider(user.getProvider())
                .providerId(user.getProviderId())
                .email(user.getEmail())
                .nickname(user.getNickname())
                .profileImageUrl(user.getProfileImageUrl())
                .points(user.getPoints())
                .level(user.getLevel())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }
}
