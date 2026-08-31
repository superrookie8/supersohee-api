package com.supersohee.api.user.error;

/** 길이 또는 허용 문자 규칙을 어긴 닉네임. */
public class InvalidNicknameException extends RuntimeException {
    public InvalidNicknameException() {
        super("닉네임은 2~20자의 한글, 영문, 숫자, . _ - 만 사용할 수 있습니다.");
    }
}
