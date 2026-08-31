package com.supersohee.api.image.admin;

import com.supersohee.api.config.JwtUtil;
import com.supersohee.api.image.admin.dto.*;
import com.supersohee.api.image.admin.service.AdminPhotoContent;
import com.supersohee.api.image.admin.service.AdminPhotoService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AdminPhotoContractIntegrationTest {
    private static final String PHOTO_ID = "507f1f77bcf86cd799439011";

    @Autowired MockMvc mockMvc;
    @Autowired JwtUtil jwtUtil;
    @MockitoBean AdminPhotoService adminPhotoService;

    @Test
    void canonicalListUploadDeleteAndContentContractsUseCamelCase() throws Exception {
        AdminPhotoItemResponse item = new AdminPhotoItemResponse(
                PHOTO_ID, "photo.png", MediaType.IMAGE_PNG_VALUE, 9,
                Instant.parse("2026-01-01T00:00:00Z"));
        when(adminPhotoService.findAll()).thenReturn(new AdminPhotoListResponse(List.of(item), List.of()));
        when(adminPhotoService.upload(anyList()))
                .thenReturn(new AdminPhotoUploadResponse("Photos uploaded successfully.", List.of(item)));
        when(adminPhotoService.delete(List.of(PHOTO_ID)))
                .thenReturn(new AdminPhotoDeleteResponse(
                        "Photos deleted successfully.", List.of(PHOTO_ID), List.of()));
        byte[] bytes = {(byte) 0x89, 0x50, 0x4E, 0x47};
        when(adminPhotoService.getContent(PHOTO_ID)).thenReturn(
                new AdminPhotoContent(new ByteArrayResource(bytes), MediaType.IMAGE_PNG, bytes.length));

        mockMvc.perform(get("/api/admin/photos").header(HttpHeaders.AUTHORIZATION, adminBearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.adminPhotos[0].id").value(PHOTO_ID))
                .andExpect(jsonPath("$.adminPhotos[0].filename").value("photo.png"))
                .andExpect(jsonPath("$.userPhotos").isArray());

        MockMultipartFile photo = new MockMultipartFile(
                "photos", "photo.png", MediaType.IMAGE_PNG_VALUE, bytes);
        mockMvc.perform(multipart("/api/admin/photos").file(photo)
                        .header(HttpHeaders.AUTHORIZATION, adminBearer()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.photos[0].contentType").value(MediaType.IMAGE_PNG_VALUE));

        mockMvc.perform(delete("/api/admin/photos")
                        .header(HttpHeaders.AUTHORIZATION, adminBearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"photoIds\":[\"" + PHOTO_ID + "\"]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.deletedIds[0]").value(PHOTO_ID))
                .andExpect(jsonPath("$.failedIds").isEmpty());

        mockMvc.perform(get("/api/admin/photos/{id}/content", PHOTO_ID)
                        .header(HttpHeaders.AUTHORIZATION, adminBearer()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.IMAGE_PNG))
                .andExpect(content().bytes(bytes))
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, "no-store"));
    }

    @Test
    void adminBoundaryAndDeleteValidationAreEnforced() throws Exception {
        mockMvc.perform(get("/api/admin/photos"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("ADMIN_AUTHENTICATION_REQUIRED"));
        mockMvc.perform(get("/api/admin/photos")
                        .header(HttpHeaders.AUTHORIZATION, userBearer()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ADMIN_ACCESS_DENIED"));
        mockMvc.perform(get("/api/admin/photos/{id}/content", PHOTO_ID)
                        .header(HttpHeaders.AUTHORIZATION, userBearer()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ADMIN_ACCESS_DENIED"));
        mockMvc.perform(delete("/api/admin/photos")
                        .header(HttpHeaders.AUTHORIZATION, adminBearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"photoIds\":[]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("ADMIN_VALIDATION_FAILED"))
                .andExpect(jsonPath("$.fieldErrors.photoIds").exists());
    }

    @Test
    void partialDeleteUsesNonSuccessStatusAndExplicitFailedIds() throws Exception {
        when(adminPhotoService.delete(List.of(PHOTO_ID))).thenReturn(new AdminPhotoDeleteResponse(
                "Some photos could not be deleted.", List.of(), List.of(PHOTO_ID)));

        mockMvc.perform(delete("/api/admin/photos")
                        .header(HttpHeaders.AUTHORIZATION, adminBearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"photoIds\":[\"" + PHOTO_ID + "\"]}"))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.deletedIds").isEmpty())
                .andExpect(jsonPath("$.failedIds[0]").value(PHOTO_ID));
    }

    private String adminBearer() {
        return "Bearer " + jwtUtil.generateAdminToken("admin");
    }

    private String userBearer() {
        return "Bearer " + jwtUtil.generateUserToken("user-1");
    }
}
