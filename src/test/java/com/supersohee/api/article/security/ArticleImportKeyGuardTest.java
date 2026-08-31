package com.supersohee.api.article.security;

import com.supersohee.api.admin.error.AdminApiException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

class ArticleImportKeyGuardTest {
    private static final String VALID_KEY = "a-secure-import-key-that-is-at-least-thirty-two-bytes";

    @Test
    void rejectsMissingOrWeakConfiguredKeyAtStartup() {
        assertThatIllegalStateException().isThrownBy(() -> new ArticleImportKeyGuard(null));
        assertThatIllegalStateException().isThrownBy(() -> new ArticleImportKeyGuard("too-short"));
    }

    @Test
    void acceptsOnlyTheExactConfiguredKey() {
        ArticleImportKeyGuard guard = new ArticleImportKeyGuard(VALID_KEY);

        guard.verify(VALID_KEY);
        assertThatExceptionOfType(AdminApiException.class)
                .isThrownBy(() -> guard.verify(null))
                .satisfies(exception -> {
                    assertThat(exception.status()).isEqualTo(HttpStatus.UNAUTHORIZED);
                    assertThat(exception.code()).isEqualTo("ADMIN_AUTHENTICATION_REQUIRED");
                });
        assertThatExceptionOfType(AdminApiException.class)
                .isThrownBy(() -> guard.verify(VALID_KEY + "-wrong"))
                .satisfies(exception -> assertThat(exception.status()).isEqualTo(HttpStatus.UNAUTHORIZED));
    }
}
