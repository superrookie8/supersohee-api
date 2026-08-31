package com.supersohee.api.article.security;

import com.supersohee.api.admin.error.AdminApiException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

@Component
public class ArticleImportKeyGuard {
    private final byte[] expectedKey;

    public ArticleImportKeyGuard(
            @Value("${article.import.key:${SUPERSOHEE_ARTICLE_IMPORT_KEY:}}") String key) {
        if (key == null || key.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException("article.import.key must be at least 32 bytes");
        }
        this.expectedKey = key.getBytes(StandardCharsets.UTF_8);
    }

    public void verify(String presentedKey) {
        byte[] presented = presentedKey == null
                ? new byte[0]
                : presentedKey.getBytes(StandardCharsets.UTF_8);
        if (!MessageDigest.isEqual(expectedKey, presented)) {
            throw AdminApiException.unauthorized("A valid article import key is required.");
        }
    }
}
