package com.supersohee.api.user;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.supersohee.api.user.domain.User;
import com.supersohee.api.user.dto.SignupRequest;
import com.supersohee.api.user.dto.UpdateMyProfileRequest;
import com.supersohee.api.user.repository.UserRepository;
import com.supersohee.api.user.service.UserService;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class UserAdminRoleTest {
    @Test void profileAndProviderSyncPreserveServerOwnedRole() {
        User user = User.builder().id("fixture").role("ADMIN").nickname("nick").build();
        assertThat(user.syncProviderProfile("fixture@example.com", "provider", null).getRole()).isEqualTo("ADMIN");
        assertThat(user.withProfileEdits("new nickname", null).getRole()).isEqualTo("ADMIN");
        assertThat(User.builder().id("ordinary").build().getRole()).isNull();
    }

    @Test void signupAndProfileDtosCannotBindAdministratorRole() throws Exception {
        var mapper = new ObjectMapper().disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        var signup = mapper.readValue("{\"email\":\"fixture@example.com\",\"password\":\"fixture-password\",\"passwordConfirm\":\"fixture-password\",\"nickname\":\"fixture\",\"role\":\"ADMIN\"}", SignupRequest.class);
        var profile = mapper.readValue("{\"nickname\":\"fixture\",\"role\":\"ADMIN\"}", UpdateMyProfileRequest.class);
        assertThat(mapper.writeValueAsString(signup)).doesNotContain("role", "ADMIN");
        assertThat(mapper.writeValueAsString(profile)).doesNotContain("role", "ADMIN");
    }
}
