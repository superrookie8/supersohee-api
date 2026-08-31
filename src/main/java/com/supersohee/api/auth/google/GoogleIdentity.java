package com.supersohee.api.auth.google;

public record GoogleIdentity(
        String subject,
        String email,
        String name,
        String picture) {
}
