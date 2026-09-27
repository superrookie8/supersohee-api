package com.supersohee.api.video;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.supersohee.api.admin.error.AdminApiExceptionHandler;
import com.supersohee.api.article.security.*;
import com.supersohee.api.config.*;
import com.supersohee.api.user.domain.User;
import com.supersohee.api.user.repository.UserRepository;
import com.supersohee.api.video.controller.*;
import com.supersohee.api.video.repository.VideoRepository;
import com.supersohee.api.video.service.VideoService;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.cors.CorsConfigurationSource;

import java.time.Instant;
import java.util.*;

@org.springframework.test.context.ActiveProfiles("test")
@WebMvcTest({VideoController.class, AdminVideoController.class})
@Import({
    VideoService.class,
    SecurityConfig.class,
    JwtAuthenticationFilter.class,
    ArticleImportKeyAuthenticationFilter.class,
    AdminApiExceptionHandler.class
})
class VideoContractTest {
    @Autowired MockMvc mvc;
    @MockitoBean com.supersohee.api.monitoring.slack.SlackErrorAlertService slackErrorAlertService;
    @MockitoBean VideoRepository repository;
    @MockitoBean JwtUtil jwt;
    @MockitoBean UserRepository users;
    @MockitoBean ArticleImportKeyGuard importKey;
    @MockitoBean(name = "corsConfigurationSource") CorsConfigurationSource corsConfigurationSource;

    private void login(String role) {
        when(jwt.parseAndValidateToken("fixture-user"))
                .thenReturn(
                        new JwtUtil.JwtPrincipal(
                                "fixture", JwtUtil.ROLE_USER, "USER_ACCESS", Instant.now()));
        when(users.findById("fixture"))
                .thenReturn(Optional.of(User.builder().id("fixture").role(role).build()));
    }

    @Test
    void publicIsOpenOnlyForGetAndInvalidInputsAreSafe400() throws Exception {
        when(repository.findByPublishedTrue()).thenReturn(List.of());
        mvc.perform(get("/api/videos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.videos").isArray())
                .andExpect(jsonPath("$.total").value(0));
        for (String query : List.of("category=bad", "sort=bad", "page=-1", "page=x", "limit=101")) {
            mvc.perform(get("/api/videos?" + query))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.traceId").isNotEmpty())
                    .andExpect(jsonPath("$.fieldErrors").isMap());
        }
        mvc.perform(post("/api/videos")).andExpect(status().isUnauthorized());
    }

    @Test
    void adminRejectsAnonymousOrdinaryMemberImportKeyAndLegacyToken() throws Exception {
        mvc.perform(get("/api/admin/videos")).andExpect(status().isUnauthorized());
        mvc.perform(
                        post("/api/admin/videos/import")
                                .header("X-Article-Import-Key", "fixture-key")
                                .contentType("application/json")
                                .content("{\"videos\":[]}"))
                .andExpect(status().isUnauthorized());
        login("USER");
        mvc.perform(get("/api/admin/videos").header("Authorization", "Bearer fixture-user"))
                .andExpect(status().isForbidden());
        when(jwt.parseAndValidateToken("fixture-legacy"))
                .thenReturn(
                        new JwtUtil.JwtPrincipal(
                                "fixture", JwtUtil.ROLE_ADMIN, "ADMIN_ACCESS", Instant.now()));
        mvc.perform(get("/api/admin/videos").header("Authorization", "Bearer fixture-legacy"))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(repository);
    }

    @Test
    void adminRoleIsCheckedAndMalformedDatesRejectBeforeWrite() throws Exception {
        login("ADMIN");
        when(repository.findAll()).thenReturn(List.of());
        mvc.perform(get("/api/admin/videos").header("Authorization", "Bearer fixture-user"))
                .andExpect(status().isOk());
        mvc.perform(
                        post("/api/admin/videos")
                                .header("Authorization", "Bearer fixture-user")
                                .contentType("application/json")
                                .content("{\"eventDate\":\"2026-99-99\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("ADMIN_INVALID_REQUEST"));
        verify(repository, never()).insert(any(com.supersohee.api.video.domain.Video.class));
        login("USER");
        mvc.perform(get("/api/admin/videos").header("Authorization", "Bearer fixture-user"))
                .andExpect(status().isForbidden());
    }
}
