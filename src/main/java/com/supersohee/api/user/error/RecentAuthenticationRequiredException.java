package com.supersohee.api.user.error;

public class RecentAuthenticationRequiredException extends RuntimeException {
    public RecentAuthenticationRequiredException() {
        super("최근 인증이 필요합니다.");
    }
}
