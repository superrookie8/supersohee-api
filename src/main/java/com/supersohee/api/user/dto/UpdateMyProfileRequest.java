package com.supersohee.api.user.dto;

import jakarta.validation.constraints.Size;

/**
 * 마이페이지 프로필 수정 요청.
 *
 * 두 필드 모두 null이면 "변경하지 않음"을 뜻한다. profileImageUrl은 빈 문자열로
 * 보내면 기본 이미지로 되돌린다. 이미지 파일 자체는 /api/images/upload로 먼저
 * 올리고 그 URL만 여기로 보낸다.
 */
public record UpdateMyProfileRequest(
        @Size(min = 2, max = 20) String nickname,
        @Size(max = 2048) String profileImageUrl) {
}
