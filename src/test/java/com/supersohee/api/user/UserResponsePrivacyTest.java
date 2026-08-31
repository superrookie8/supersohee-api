package com.supersohee.api.user;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.supersohee.api.user.domain.User;
import com.supersohee.api.user.dto.PublicUserResponse;
import com.supersohee.api.user.dto.UserResponse;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * /api/users/{userId}는 비로그인에도 열려 있다. 직렬화 결과를 검사해,
 * 필드를 되돌리거나 새로 추가할 때 개인정보가 다시 새지 않도록 고정한다.
 */
class UserResponsePrivacyTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private User user() {
        return User.builder()
                .id("user-1")
                .provider("google")
                .providerId("109876543210987654321")
                .email("someone@example.com")
                .nickname("팬614773")
                .profileImageUrl("https://lh3.googleusercontent.com/a/photo=s96-c")
                .password("$2a$10$hashed")
                .points(30)
                .level(2)
                .build();
    }

    @Test
    void publicProfileHidesEmailProviderSubjectAndPhoto() throws Exception {
        String json = MAPPER.writeValueAsString(PublicUserResponse.from(user()));

        assertThat(json).doesNotContain("someone@example.com");
        assertThat(json).doesNotContain("109876543210987654321");
        assertThat(json).doesNotContain("googleusercontent.com");
        assertThat(json).doesNotContain("hashed");

        assertThat(MAPPER.readTree(json).fieldNames()).toIterable()
                .containsExactlyInAnyOrder("id", "nickname", "points", "level");
    }

    @Test
    void ownProfileKeepsEmailButNeverShipsTheProviderSubjectOrPassword() throws Exception {
        String json = MAPPER.writeValueAsString(UserResponse.from(user()));

        assertThat(json).contains("someone@example.com");
        assertThat(json).doesNotContain("109876543210987654321");
        assertThat(json).doesNotContain("hashed");
        assertThat(MAPPER.readTree(json).has("providerId")).isFalse();
    }
}
