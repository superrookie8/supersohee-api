package com.supersohee.api.user.error;

/** 다른 사용자가 이미 쓰고 있는 닉네임을 요청한 경우. */
public class NicknameAlreadyUsedException extends RuntimeException {
    public NicknameAlreadyUsedException() {
        super("이미 사용 중인 닉네임입니다.");
    }
}
