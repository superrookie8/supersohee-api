package com.supersohee.api.user;

import com.supersohee.api.config.JwtUtil;
import com.supersohee.api.user.domain.User;
import com.supersohee.api.user.error.AccountDeletionUnavailableException;
import com.supersohee.api.user.error.RecentAuthenticationRequiredException;
import com.supersohee.api.user.repository.UserRepository;
import com.supersohee.api.user.service.UserWithdrawalService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class UserWithdrawalContractIntegrationTest {
    @Autowired MockMvc mockMvc;
    @Autowired JwtUtil jwtUtil;
    @MockitoBean UserRepository userRepository;
    @MockitoBean UserWithdrawalService withdrawalService;

    @BeforeEach
    void authenticatedUserExists() {
        when(userRepository.findById("user-1")).thenReturn(Optional.of(User.builder().id("user-1").build()));
    }

    @Test
    void exactConfirmationAndFreshUserTokenDeleteWithNoResponseBody() throws Exception {
        mockMvc.perform(delete("/api/users/me")
                        .header(HttpHeaders.AUTHORIZATION, userBearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"confirmation\":\"회원 탈퇴\"}"))
                .andExpect(status().isNoContent());

        verify(withdrawalService).withdraw(eq("user-1"), any(Instant.class));
    }

    @Test
    void rejectsWrongConfirmationBeforeDeletion() throws Exception {
        mockMvc.perform(delete("/api/users/me")
                        .header(HttpHeaders.AUTHORIZATION, userBearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"confirmation\":\"탈퇴\"}"))
                .andExpect(status().isBadRequest());

        verify(withdrawalService, never()).withdraw(eq("user-1"), any());
    }

    @Test
    void mapsRecentAuthenticationAndStoragePreconditionsWithoutLeakingDetails() throws Exception {
        doThrow(new RecentAuthenticationRequiredException())
                .when(withdrawalService).withdraw(eq("user-1"), any());
        mockMvc.perform(delete("/api/users/me")
                        .header(HttpHeaders.AUTHORIZATION, userBearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"confirmation\":\"회원 탈퇴\"}"))
                .andExpect(status().isPreconditionFailed())
                .andExpect(jsonPath("$.message").value("최근 인증이 필요합니다."));

        doThrow(new AccountDeletionUnavailableException())
                .when(withdrawalService).withdraw(eq("user-1"), any());
        mockMvc.perform(delete("/api/users/me")
                        .header(HttpHeaders.AUTHORIZATION, userBearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"confirmation\":\"회원 탈퇴\"}"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.message").value("회원 탈퇴를 완료할 수 없습니다. 잠시 후 다시 시도해주세요."));
    }

    @Test
    void deletedUsersExistingJwtIsRejectedImmediately() throws Exception {
        when(userRepository.findById("user-1")).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/users/me").header(HttpHeaders.AUTHORIZATION, userBearer()))
                .andExpect(status().isUnauthorized());
    }

    private String userBearer() {
        return "Bearer " + jwtUtil.generateUserToken("user-1");
    }
}
