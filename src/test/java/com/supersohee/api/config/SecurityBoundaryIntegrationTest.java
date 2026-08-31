package com.supersohee.api.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SecurityBoundaryIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtUtil jwtUtil;

    @Test
    void adminApiRejectsAnonymousRequest() throws Exception {
        mockMvc.perform(get("/api/admin/security-boundary-probe"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void adminApiRejectsUserToken() throws Exception {
        mockMvc.perform(get("/api/admin/security-boundary-probe")
                        .header(HttpHeaders.AUTHORIZATION, bearer(jwtUtil.generateUserToken("user-1"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminTokenPassesCentralRoleCheck() throws Exception {
        mockMvc.perform(get("/api/admin/security-boundary-probe")
                        .header(HttpHeaders.AUTHORIZATION, bearer(jwtUtil.generateAdminToken("admin"))))
                .andExpect(status().isNotFound());
    }

    @Test
    void userApiRejectsAdminToken() throws Exception {
        mockMvc.perform(get("/api/users/me")
                        .header(HttpHeaders.AUTHORIZATION, bearer(jwtUtil.generateAdminToken("admin"))))
                .andExpect(status().isForbidden());
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }
}
