package com.supersohee.api.auth.google;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.supersohee.api.config.JwtUtil;
import com.supersohee.api.user.domain.User;
import com.supersohee.api.user.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class GoogleAuthExchangeIntegrationTest {

    private static final String EXCHANGE_KEY = "test-exchange-key-that-is-at-least-thirty-two-bytes";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtUtil jwtUtil;

    @MockitoBean
    private GoogleIdTokenVerifier idTokenVerifier;

    @MockitoBean
    private UserService userService;

    @Test
    void exchangesVerifiedGoogleIdentityForUserTokenAndAdminApiRejectsIt() throws Exception {
        when(idTokenVerifier.verify("valid-id-token"))
                .thenReturn(new GoogleIdentity(
                        "google-subject",
                        "fan@example.test",
                        "Fan",
                        null));
        when(userService.findOrCreateUser(
                "google",
                "google-subject",
                "fan@example.test",
                "Fan",
                null))
                .thenReturn(User.builder().id("user-1").build());

        MvcResult result = mockMvc.perform(post("/api/auth/google/exchange")
                        .header("X-Supersohee-Exchange-Key", EXCHANGE_KEY)
                        .header("Idempotency-Key", "integration-success")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idToken\":\"valid-id-token\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.token").doesNotExist())
                .andExpect(jsonPath("$.userId").value("user-1"))
                .andReturn();

        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        String token = body.get("accessToken").asText();
        assertThat(jwtUtil.parseAndValidateToken(token).role()).isEqualTo(JwtUtil.ROLE_USER);

        mockMvc.perform(get("/api/admin/security-boundary-probe")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    void rejectsMissingExchangeSecretWithSafeStructuredError() throws Exception {
        mockMvc.perform(post("/api/auth/google/exchange")
                        .header("Idempotency-Key", "integration-no-secret")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idToken\":\"unused-token\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("EXCHANGE_UNAUTHORIZED"))
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.message").isNotEmpty())
                .andExpect(jsonPath("$.traceId").isNotEmpty());
    }

    @Test
    void returnsSafeErrorForInvalidGoogleToken() throws Exception {
        when(idTokenVerifier.verify("invalid-id-token"))
                .thenThrow(new InvalidGoogleTokenException());

        mockMvc.perform(post("/api/auth/google/exchange")
                        .header("X-Supersohee-Exchange-Key", EXCHANGE_KEY)
                        .header("Idempotency-Key", "integration-invalid-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idToken\":\"invalid-id-token\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_GOOGLE_TOKEN"))
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.traceId").isNotEmpty());
    }
}
