package com.supersohee.api.user.error;

public class AccountDeletionUnavailableException extends RuntimeException {
    public AccountDeletionUnavailableException() {
        super("회원 탈퇴를 완료할 수 없습니다. 잠시 후 다시 시도해주세요.");
    }
}
