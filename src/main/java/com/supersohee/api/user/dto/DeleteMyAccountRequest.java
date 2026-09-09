package com.supersohee.api.user.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record DeleteMyAccountRequest(
        @NotNull
        @Pattern(regexp = "\\Q회원 탈퇴\\E", message = "탈퇴 확인 문구가 일치하지 않습니다.")
        String confirmation) {
}
