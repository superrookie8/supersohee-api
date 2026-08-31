package com.supersohee.api.user.dto;

import com.supersohee.api.user.domain.User;

/**
 * 다른 사용자에게 보여도 되는 최소 프로필.
 *
 * /api/users/{userId}는 비로그인에도 열려 있다. 이전에는 UserResponse를 그대로
 * 내보내 email과 providerId(구글 sub)까지 공개됐다. 여기에는 이미 공개 랭킹에
 * 나가는 수준의 정보만 담는다.
 *
 * profileImageUrl은 제외한다. 저장된 값이 구글 계정 사진 URL일 수 있어 본인이
 * 올린 사진과 구분되지 않는다.
 */
public record PublicUserResponse(
        String id,
        String nickname,
        Integer points,
        Integer level) {

    public static PublicUserResponse from(User user) {
        return new PublicUserResponse(
                user.getId(),
                user.getNickname(),
                user.getPoints(),
                user.getLevel());
    }
}
