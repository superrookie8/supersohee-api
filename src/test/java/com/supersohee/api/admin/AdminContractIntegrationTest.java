package com.supersohee.api.admin;

import com.supersohee.api.admin.error.AdminApiException;
import com.supersohee.api.config.JwtUtil;
import com.supersohee.api.event.domain.Event;
import com.supersohee.api.event.dto.AdminEventOrderResponse;
import com.supersohee.api.event.service.EventService;
import com.supersohee.api.image.service.ImageUploadService;
import com.supersohee.api.image.service.ImageValidationException;
import com.supersohee.api.playerstat.domain.PlayerStat;
import com.supersohee.api.playerstat.service.PlayerStatService;
import com.supersohee.api.schedule.domain.Schedule;
import com.supersohee.api.schedule.service.ScheduleService;
import com.supersohee.api.player.domain.Player;
import com.supersohee.api.player.service.PlayerService;
import com.supersohee.api.guestbook.domain.Guestbook;
import com.supersohee.api.guestbook.service.GuestbookPhoto;
import com.supersohee.api.guestbook.service.GuestbookPhotoService;
import com.supersohee.api.guestbook.service.GuestbookService;
import com.supersohee.api.article.domain.Article;
import com.supersohee.api.article.dto.AdminArticleImportResponse;
import com.supersohee.api.article.service.ArticleService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AdminContractIntegrationTest {
    private static final String ARTICLE_IMPORT_KEY =
            "test-article-import-key-that-is-at-least-thirty-two-bytes";

    @Autowired MockMvc mockMvc;
    @Autowired JwtUtil jwtUtil;

    @MockitoBean EventService eventService;
    @MockitoBean ImageUploadService imageUploadService;
    @MockitoBean ScheduleService scheduleService;
    @MockitoBean PlayerStatService playerStatService;
    @MockitoBean PlayerService playerService;
    @MockitoBean GuestbookService guestbookService;
    @MockitoBean GuestbookPhotoService guestbookPhotoService;
    @MockitoBean ArticleService articleService;

    @Test
    void existingAdminLoginStillIssuesAdminToken() throws Exception {
        mockMvc.perform(post("/api/admin/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"test-admin\",\"password\":\"test-admin-password\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.role").value("ADMIN"));
    }

    @Test
    void profileSingletonGetAndPutUseCanonicalFieldsAndJerseyRange() throws Exception {
        Player player = Player.builder()
                .id("player-1").name("이소희").team("BNK 썸").position("G")
                .jerseyNumber(6).nationalTeamJerseyNumber(9).height("171cm").nickname(List.of("소히"))
                .features("빠른 가드").profileImageUrl("https://image.test/profile").build();
        when(playerService.findSohee()).thenReturn(java.util.Optional.of(player));
        when(playerService.updateSohee(any())).thenReturn(player);
        String body = """
                {"name":"이소희","team":"BNK 썸","position":"G","jerseyNumber":6,"nationalTeamJerseyNumber":9,"height":"171cm","nicknames":["소히"],"features":"빠른 가드","profileImageUrl":"https://image.test/profile"}
                """;

        mockMvc.perform(get("/api/admin/profile").header(HttpHeaders.AUTHORIZATION, adminBearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("player-1"))
                .andExpect(jsonPath("$.jerseyNumber").value(6))
                .andExpect(jsonPath("$.nationalTeamJerseyNumber").value(9))
                .andExpect(jsonPath("$.nicknames[0]").value("소히"));
        mockMvc.perform(put("/api/admin/profile").contentType(MediaType.APPLICATION_JSON).content(body)
                        .header(HttpHeaders.AUTHORIZATION, adminBearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.profileImageUrl").value("https://image.test/profile"));
        String legacyBody = """
                {"name":"이소희","team":"BNK 썸","position":"G","number":6,"height":"171cm","nickname":"소히","features":"빠른 가드"}
                """;
        mockMvc.perform(put("/api/admin/profile").contentType(MediaType.APPLICATION_JSON).content(legacyBody)
                        .header(HttpHeaders.AUTHORIZATION, adminBearer()))
                .andExpect(status().isOk());

        mockMvc.perform(put("/api/admin/profile").contentType(MediaType.APPLICATION_JSON)
                        .content(body.replace("\"jerseyNumber\":6", "\"jerseyNumber\":136"))
                        .header(HttpHeaders.AUTHORIZATION, adminBearer()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.jerseyNumber").exists());
    }

    @Test
    void guestbookSearchPaginationAndDeleteContractsAreAdminOnly() throws Exception {
        Guestbook entry = Guestbook.builder()
                .id("guest-1").name("팬").message("응원합니다")
                .date(LocalDateTime.of(2026, 1, 1, 12, 0)).legacyPhotoId("legacy-photo").build();
        when(guestbookService.findAdminGuestbooks(0, 20, "팬"))
                .thenReturn(new PageImpl<>(List.of(entry), PageRequest.of(0, 20), 1));

        mockMvc.perform(get("/api/admin/guestbooks").queryParam("name", "팬")
                        .header(HttpHeaders.AUTHORIZATION, adminBearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value("guest-1"))
                .andExpect(jsonPath("$.content[0].hasPhoto").value(true))
                .andExpect(jsonPath("$.totalElements").value(1));
        mockMvc.perform(delete("/api/admin/guestbooks/guest-1")
                        .header(HttpHeaders.AUTHORIZATION, adminBearer()))
                .andExpect(status().isNoContent());
        verify(guestbookService).delete("guest-1");
        mockMvc.perform(get("/api/admin/guestbooks").queryParam("size", "101")
                        .header(HttpHeaders.AUTHORIZATION, adminBearer()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("ADMIN_INVALID_REQUEST"));
    }

    @Test
    void guestbookPhotoIsPrivateAndAvailableOnlyThroughItsEntry() throws Exception {
        byte[] image = new byte[]{1, 2, 3};
        when(guestbookPhotoService.getPhotoForEntry("guest-1"))
                .thenReturn(new GuestbookPhoto(new ByteArrayResource(image), MediaType.IMAGE_PNG, image.length));

        mockMvc.perform(get("/api/admin/guestbooks/guest-1/photo")
                        .header(HttpHeaders.AUTHORIZATION, adminBearer()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.IMAGE_PNG))
                .andExpect(content().bytes(image))
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, "no-store"));
        mockMvc.perform(get("/api/admin/guestbooks/guest-1/photo"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("ADMIN_AUTHENTICATION_REQUIRED"));
        mockMvc.perform(get("/api/admin/guestbooks/guest-1/photo")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtUtil.generateUserToken("user-1")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ADMIN_ACCESS_DENIED"));

        when(guestbookPhotoService.getPhotoForEntry("missing"))
                .thenThrow(AdminApiException.notFound("Guestbook photo"));
        mockMvc.perform(get("/api/admin/guestbooks/missing/photo")
                        .header(HttpHeaders.AUTHORIZATION, adminBearer()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("ADMIN_RESOURCE_NOT_FOUND"))
                .andExpect(jsonPath("$.traceId").isNotEmpty());
    }

    @Test
    void articleListManualCreateAndImportContractsAreBounded() throws Exception {
        Article article = Article.builder()
                .id("article-1").source("manual").title("소식").summary("내용")
                .publishedAt(LocalDateTime.of(2026, 1, 1, 12, 0)).build();
        when(articleService.getAdminArticles(null, 0, 20))
                .thenReturn(new PageImpl<>(List.of(article), PageRequest.of(0, 20), 1));
        when(articleService.createManualArticle(any())).thenReturn(article);
        when(articleService.importArticles(any())).thenReturn(new AdminArticleImportResponse(1, 1, 0));

        mockMvc.perform(get("/api/admin/articles").header(HttpHeaders.AUTHORIZATION, adminBearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].source").value("manual"));
        mockMvc.perform(post("/api/admin/articles").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"소식\",\"content\":\"내용\"}")
                        .header(HttpHeaders.AUTHORIZATION, adminBearer()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.summary").value("내용"));
        String importBody = """
                {"articles":[{"source":"jumpball","title":"기사","url":"https://jumpball.test/1","summary":"요약","publishedAt":"2026-01-01T12:00:00"}]}
                """;
        mockMvc.perform(post("/api/admin/articles/import").contentType(MediaType.APPLICATION_JSON)
                        .header("X-Article-Import-Key", ARTICLE_IMPORT_KEY)
                        .content(importBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.processed").value(1))
                .andExpect(jsonPath("$.created").value(1));
        mockMvc.perform(post("/api/admin/articles/import").contentType(MediaType.APPLICATION_JSON)
                        .header("X-Article-Import-Key", ARTICLE_IMPORT_KEY)
                        .content(importBody.replace("jumpball", "unknown")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("ADMIN_VALIDATION_FAILED"));
        mockMvc.perform(post("/api/admin/articles/import").contentType(MediaType.APPLICATION_JSON)
                        .header(HttpHeaders.AUTHORIZATION, adminBearer())
                        .content(importBody))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("ADMIN_AUTHENTICATION_REQUIRED"));
        mockMvc.perform(post("/api/admin/articles/import").contentType(MediaType.APPLICATION_JSON)
                        .header("X-Article-Import-Key", "wrong-key")
                        .content(importBody))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("ADMIN_AUTHENTICATION_REQUIRED"));

        mockMvc.perform(get("/api/admin/articles")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtUtil.generateUserToken("user-1")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ADMIN_ACCESS_DENIED"));
    }

    @Test
    void articleImportAcceptsTwoHundredAndRejectsTwoHundredOneItems() throws Exception {
        when(articleService.importArticles(any())).thenAnswer(invocation -> {
            int count = ((com.supersohee.api.article.dto.AdminArticleImportRequest)
                    invocation.getArgument(0)).articles().size();
            return new AdminArticleImportResponse(count, count, 0);
        });

        mockMvc.perform(post("/api/admin/articles/import")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Article-Import-Key", ARTICLE_IMPORT_KEY)
                        .content(articleImportPayload(200)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.processed").value(200));

        mockMvc.perform(post("/api/admin/articles/import")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Article-Import-Key", ARTICLE_IMPORT_KEY)
                        .content(articleImportPayload(201)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("ADMIN_VALIDATION_FAILED"))
                .andExpect(jsonPath("$.fieldErrors.articles").exists());
    }

    @Test
    void adminEventContractUsesArrayIdCamelCaseAndMultipart() throws Exception {
        Event event = Event.builder()
                .id("event-1")
                .title("Fan event")
                .check1("Bring ticket")
                .photoKeys(List.of("event/photo.png"))
                .isActive(true)
                .build();
        when(eventService.findAllEvents()).thenReturn(List.of(event));
        when(imageUploadService.convertKeysToPresignedUrls(anyList())).thenReturn(List.of("https://image.test/photo"));

        mockMvc.perform(get("/api/admin/events").header(HttpHeaders.AUTHORIZATION, adminBearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("event-1"))
                .andExpect(jsonPath("$[0].checkFields.check1").value("Bring ticket"))
                .andExpect(jsonPath("$[0].photoKeys[0]").value("event/photo.png"));

        when(imageUploadService.uploadEventImages(anyList())).thenReturn(List.of("event/new.png"));
        when(eventService.createEvent(eq("New event"), any(), any(), eq("Check"), any(), any(), anyList(), eq(true)))
                .thenReturn(event);
        MockMultipartFile photo = new MockMultipartFile("photos", "photo.png", "image/png", new byte[]{1});

        mockMvc.perform(multipart("/api/admin/events")
                        .file(photo)
                        .param("title", "New event")
                        .param("check1", "Check")
                        .header(HttpHeaders.AUTHORIZATION, adminBearer()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value("event-1"))
                .andExpect(jsonPath("$.isActive").value(true));
    }

    @Test
    void adminUiEventMultipartFixtureUsesDefaultsAndBlankOptionalFields() throws Exception {
        Event event = Event.builder().id("event-ui").title("테스트 이벤트").photoKeys(List.of("event/ui.png"))
                .isActive(true).build();
        when(imageUploadService.uploadEventImages(anyList())).thenReturn(List.of("event/ui.png"));
        when(eventService.createEvent(
                eq("테스트 이벤트"), isNull(), eq("fixture"), eq(""), isNull(), isNull(),
                eq(List.of("event/ui.png")), eq(true))).thenReturn(event);
        MockMultipartFile photo = new MockMultipartFile(
                "photos", "fixture.png", MediaType.IMAGE_PNG_VALUE,
                new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47});

        mockMvc.perform(multipart("/api/admin/events")
                        .file(photo)
                        .param("title", "테스트 이벤트")
                        .param("description", "fixture")
                        .param("check1", "")
                        .header(HttpHeaders.AUTHORIZATION, adminBearer()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value("event-ui"))
                .andExpect(jsonPath("$.isActive").value(true));

        verify(eventService).createEvent(
                eq("테스트 이벤트"), isNull(), eq("fixture"), eq(""), isNull(), isNull(),
                eq(List.of("event/ui.png")), eq(true));
    }

    @Test
    void eventPhotoValidationReturnsSafeFieldSpecificErrors() throws Exception {
        when(imageUploadService.uploadEventImages(anyList()))
                .thenThrow(new ImageValidationException(ImageValidationException.Reason.TOO_LARGE))
                .thenThrow(new ImageValidationException(ImageValidationException.Reason.UNSUPPORTED_FORMAT));
        MockMultipartFile photo = new MockMultipartFile(
                "photos", "photo.png", MediaType.IMAGE_PNG_VALUE, new byte[]{1});

        mockMvc.perform(multipart("/api/admin/events")
                        .file(photo).param("title", "Oversized")
                        .header(HttpHeaders.AUTHORIZATION, adminBearer()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("ADMIN_VALIDATION_FAILED"))
                .andExpect(jsonPath("$.fieldErrors.photos").value("Each photo must be 5 MiB or smaller."))
                .andExpect(jsonPath("$.traceId").isNotEmpty());

        mockMvc.perform(multipart("/api/admin/events")
                        .file(photo).param("title", "Unsupported")
                        .header(HttpHeaders.AUTHORIZATION, adminBearer()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("ADMIN_VALIDATION_FAILED"))
                .andExpect(jsonPath("$.fieldErrors.photos")
                        .value("Photos must use jpg, jpeg, png, gif, or webp format."));
    }

    @Test
    void eventEditReplacesMetadataAndAppendsNewWebpPhotos() throws Exception {
        Event updated = Event.builder()
                .id("event-1")
                .title("Updated event")
                .photoKeys(List.of("event/existing.webp", "event/new.webp"))
                .isActive(false)
                .build();
        when(imageUploadService.uploadEventImages(anyList())).thenReturn(List.of("event/new.webp"));
        when(eventService.updateEvent(
                eq("event-1"), eq("Updated event"), isNull(), isNull(), isNull(), isNull(), isNull(),
                eq(List.of("event/new.webp")), eq(false))).thenReturn(updated);
        when(imageUploadService.convertKeysToPresignedUrls(updated.getPhotoKeys()))
                .thenReturn(List.of("https://image.test/existing", "https://image.test/new"));
        MockMultipartFile webp = new MockMultipartFile(
                "photos", "new.webp", "image/webp",
                new byte[]{'R', 'I', 'F', 'F', 1, 2, 3, 4, 'W', 'E', 'B', 'P'});

        mockMvc.perform(multipart(HttpMethod.PUT, "/api/admin/events/event-1")
                        .file(webp)
                        .param("title", " Updated event ")
                        .param("url", "")
                        .param("description", "")
                        .param("check1", "")
                        .param("check2", "")
                        .param("check3", "")
                        .param("isActive", "false")
                        .header(HttpHeaders.AUTHORIZATION, adminBearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("event-1"))
                .andExpect(jsonPath("$.title").value("Updated event"))
                .andExpect(jsonPath("$.url").doesNotExist())
                .andExpect(jsonPath("$.description").doesNotExist())
                .andExpect(jsonPath("$.photoKeys[0]").value("event/existing.webp"))
                .andExpect(jsonPath("$.photoKeys[1]").value("event/new.webp"))
                .andExpect(jsonPath("$.photos[1]").value("https://image.test/new"))
                .andExpect(jsonPath("$.isActive").value(false));

        verify(eventService).updateEvent(
                eq("event-1"), eq("Updated event"), isNull(), isNull(), isNull(), isNull(), isNull(),
                eq(List.of("event/new.webp")), eq(false));
    }

    @Test
    void eventMetadataOnlyEditAndValidationUseTheCanonicalMultipartContract() throws Exception {
        Event updated = Event.builder()
                .id("event-1").title("Metadata only")
                .photoKeys(List.of("event/existing.webp")).isActive(true).build();
        when(eventService.updateEvent(
                eq("event-1"), eq("Metadata only"), isNull(), isNull(), isNull(), isNull(), isNull(),
                eq(List.of()), eq(true))).thenReturn(updated);
        when(imageUploadService.convertKeysToPresignedUrls(updated.getPhotoKeys()))
                .thenReturn(List.of("https://image.test/existing"));

        mockMvc.perform(multipart(HttpMethod.PUT, "/api/admin/events/event-1")
                        .param("title", "Metadata only")
                        .param("isActive", "true")
                        .header(HttpHeaders.AUTHORIZATION, adminBearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.photoKeys[0]").value("event/existing.webp"));

        mockMvc.perform(multipart(HttpMethod.PUT, "/api/admin/events/event-1")
                        .param("title", " ")
                        .param("isActive", "true")
                        .header(HttpHeaders.AUTHORIZATION, adminBearer()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.title").value("Event title is required."));
        mockMvc.perform(multipart(HttpMethod.PUT, "/api/admin/events/event-1")
                        .param("title", "Valid")
                        .param("isActive", "yes")
                        .header(HttpHeaders.AUTHORIZATION, adminBearer()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.isActive").value("isActive must be true or false."));
    }

    @Test
    void eventEditPhotoErrorsAreSafeAndUnknownIdsReturnNotFound() throws Exception {
        MockMultipartFile photo = new MockMultipartFile(
                "photos", "photo.webp", "image/webp",
                new byte[]{'R', 'I', 'F', 'F', 1, 2, 3, 4, 'W', 'E', 'B', 'P'});
        when(imageUploadService.uploadEventImages(anyList()))
                .thenThrow(new ImageValidationException(ImageValidationException.Reason.TOO_LARGE))
                .thenThrow(new ImageValidationException(ImageValidationException.Reason.UNSUPPORTED_FORMAT));

        mockMvc.perform(multipart(HttpMethod.PUT, "/api/admin/events/event-1")
                        .file(photo).param("title", "Edit").param("isActive", "true")
                        .header(HttpHeaders.AUTHORIZATION, adminBearer()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.photos").value("Each photo must be 5 MiB or smaller."));
        mockMvc.perform(multipart(HttpMethod.PUT, "/api/admin/events/event-1")
                        .file(photo).param("title", "Edit").param("isActive", "true")
                        .header(HttpHeaders.AUTHORIZATION, adminBearer()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.photos")
                        .value("Photos must use jpg, jpeg, png, gif, or webp format."));

        when(eventService.updateEvent(
                eq("missing"), eq("Edit"), isNull(), isNull(), isNull(), isNull(), isNull(),
                eq(List.of()), eq(true))).thenThrow(AdminApiException.notFound("Event"));
        mockMvc.perform(multipart(HttpMethod.PUT, "/api/admin/events/missing")
                        .param("title", "Edit").param("isActive", "true")
                        .header(HttpHeaders.AUTHORIZATION, adminBearer()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("ADMIN_RESOURCE_NOT_FOUND"));
    }

    @Test
    void eventEditRollsBackOnlyNewPhotosWhenDatabaseUpdateFails() throws Exception {
        when(imageUploadService.uploadEventImages(anyList()))
                .thenReturn(List.of("event/new-1.webp", "event/new-2.webp"));
        when(eventService.updateEvent(
                eq("event-1"), eq("Edit"), isNull(), isNull(), isNull(), isNull(), isNull(),
                eq(List.of("event/new-1.webp", "event/new-2.webp")), eq(true)))
                .thenThrow(new IllegalStateException("fixture database failure"));
        MockMultipartFile photo = new MockMultipartFile(
                "photos", "photo.webp", "image/webp",
                new byte[]{'R', 'I', 'F', 'F', 1, 2, 3, 4, 'W', 'E', 'B', 'P'});

        mockMvc.perform(multipart(HttpMethod.PUT, "/api/admin/events/event-1")
                        .file(photo).param("title", "Edit").param("isActive", "true")
                        .header(HttpHeaders.AUTHORIZATION, adminBearer()))
                .andExpect(status().isInternalServerError());

        verify(imageUploadService).deleteImage("event/new-1.webp");
        verify(imageUploadService).deleteImage("event/new-2.webp");
        verify(imageUploadService, never()).deleteImage("event/existing.webp");
    }

    @Test
    void eventEditIsAdminOnly() throws Exception {
        mockMvc.perform(multipart(HttpMethod.PUT, "/api/admin/events/event-1")
                        .param("title", "Edit").param("isActive", "true")
                        .header(HttpHeaders.AUTHORIZATION, userBearer()))
                .andExpect(status().isForbidden());
        mockMvc.perform(multipart(HttpMethod.PUT, "/api/admin/events/event-1")
                        .param("title", "Edit").param("isActive", "true"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void eventDeleteAndPhotoDeleteUseCanonicalPaths() throws Exception {
        Event updated = Event.builder().id("event-1").photoKeys(List.of()).isActive(true).build();
        when(eventService.deleteEventPhoto("event-1", "event/photo.png")).thenReturn(updated);

        mockMvc.perform(delete("/api/admin/events/event-1/photos")
                        .queryParam("photoKey", "event/photo.png")
                        .header(HttpHeaders.AUTHORIZATION, adminBearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("event-1"));
        mockMvc.perform(delete("/api/admin/events/event-1")
                        .header(HttpHeaders.AUTHORIZATION, adminBearer()))
                .andExpect(status().isNoContent());
        verify(eventService).deleteEvent("event-1");
    }

    @Test
    void eventOrderEndpointPersistsTheCompleteTopToBottomIdList() throws Exception {
        when(eventService.reorderEvents(List.of("event-2", "event-1")))
                .thenReturn(new AdminEventOrderResponse(
                        "Event order saved successfully.", List.of("event-2", "event-1")));

        mockMvc.perform(put("/api/admin/events/order")
                        .header(HttpHeaders.AUTHORIZATION, adminBearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"eventIds\":[\"event-2\",\"event-1\"]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.eventIds[0]").value("event-2"));
        mockMvc.perform(put("/api/admin/events/order")
                        .header(HttpHeaders.AUTHORIZATION, userBearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"eventIds\":[\"event-2\",\"event-1\"]}"))
                .andExpect(status().isForbidden());
        mockMvc.perform(put("/api/admin/events/order")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"eventIds\":[\"event-2\",\"event-1\"]}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void scheduleCrudAndSeasonContractsAreFlatAndAdditive() throws Exception {
        Schedule schedule = schedule();
        when(scheduleService.findAdminSchedules("2025-2026")).thenReturn(List.of(schedule));
        when(scheduleService.findAdminSeasons()).thenReturn(List.of("2024-2025", "2025-2026"));
        when(scheduleService.createAdminSchedule(any())).thenReturn(schedule);
        when(scheduleService.updateAdminSchedule(eq("schedule-1"), any())).thenReturn(schedule);
        String body = """
                {"season":"2025-2026","date":"2026-01-02","time":"19:00","opponent":"KB스타즈","isHome":true,"extraHome":"사직","specialGame":false,"isActive":true}
                """;

        mockMvc.perform(get("/api/admin/schedules").queryParam("season", "2025-2026")
                        .header(HttpHeaders.AUTHORIZATION, adminBearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("schedule-1"))
                .andExpect(jsonPath("$[0].date").value("2026-01-02"))
                .andExpect(jsonPath("$[0].specialGame").value(false));
        mockMvc.perform(get("/api/admin/schedules/seasons")
                        .header(HttpHeaders.AUTHORIZATION, adminBearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[1]").value("2025-2026"));
        mockMvc.perform(post("/api/admin/schedules").contentType(MediaType.APPLICATION_JSON).content(body)
                        .header(HttpHeaders.AUTHORIZATION, adminBearer()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.opponent").value("KB스타즈"));
        mockMvc.perform(put("/api/admin/schedules/schedule-1").contentType(MediaType.APPLICATION_JSON).content(body)
                        .header(HttpHeaders.AUTHORIZATION, adminBearer()))
                .andExpect(status().isOk());
        mockMvc.perform(delete("/api/admin/schedules/schedule-1")
                        .header(HttpHeaders.AUTHORIZATION, adminBearer()))
                .andExpect(status().isNoContent());
    }

    @Test
    void playerStatPostIsFlatSeasonUpsertContract() throws Exception {
        PlayerStat stat = PlayerStat.builder().id("stat-1").season("2025-2026").team("BNK 썸").gamesPlayed(10).build();
        when(playerStatService.upsertPlayerStat(any())).thenReturn(stat);
        when(playerStatService.updatePlayerStat(eq("stat-1"), any())).thenReturn(stat);
        when(playerStatService.findAllSoheeStats()).thenReturn(List.of(stat));
        String body = "{\"season\":\"2025-2026\",\"team\":\"BNK 썸\",\"gamesPlayed\":10}";

        mockMvc.perform(post("/api/admin/playerstat").contentType(MediaType.APPLICATION_JSON).content(body)
                        .header(HttpHeaders.AUTHORIZATION, adminBearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("stat-1"))
                .andExpect(jsonPath("$.gamesPlayed").value(10));
        mockMvc.perform(get("/api/admin/playerstat")
                        .header(HttpHeaders.AUTHORIZATION, adminBearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].season").value("2025-2026"));
        mockMvc.perform(put("/api/admin/playerstat/stat-1").contentType(MediaType.APPLICATION_JSON).content(body)
                        .header(HttpHeaders.AUTHORIZATION, adminBearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("stat-1"));
        mockMvc.perform(delete("/api/admin/playerstat/stat-1")
                        .header(HttpHeaders.AUTHORIZATION, adminBearer()))
                .andExpect(status().isNoContent());
        verify(playerStatService).deletePlayerStat("stat-1");
    }

    @Test
    void validationNotFoundAndConflictUseSafeAdminErrorContract() throws Exception {
        mockMvc.perform(post("/api/admin/playerstat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"season\":\"bad\",\"team\":\"\"}")
                        .header(HttpHeaders.AUTHORIZATION, adminBearer()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("ADMIN_VALIDATION_FAILED"))
                .andExpect(jsonPath("$.fieldErrors.season").exists())
                .andExpect(jsonPath("$.traceId").isNotEmpty());

        when(playerStatService.findById("missing")).thenReturn(java.util.Optional.empty());
        mockMvc.perform(get("/api/admin/playerstat/missing")
                        .header(HttpHeaders.AUTHORIZATION, adminBearer()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("ADMIN_RESOURCE_NOT_FOUND"));

        when(playerStatService.upsertPlayerStat(any())).thenThrow(AdminApiException.conflict("Season conflict."));
        mockMvc.perform(post("/api/admin/playerstat").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"season\":\"2025-2026\",\"team\":\"BNK 썸\"}")
                        .header(HttpHeaders.AUTHORIZATION, adminBearer()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    void anonymousAndUserTokenCannotUseAdminCrud() throws Exception {
        mockMvc.perform(get("/api/admin/events"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("ADMIN_AUTHENTICATION_REQUIRED"));
        mockMvc.perform(get("/api/admin/events")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtUtil.generateUserToken("user-1")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ADMIN_ACCESS_DENIED"));
    }

    private String adminBearer() {
        return "Bearer " + jwtUtil.generateAdminToken("admin");
    }

    private String userBearer() {
        return "Bearer " + jwtUtil.generateUserToken("user-1");
    }

    private String articleImportPayload(int count) {
        String articles = IntStream.range(0, count)
                .mapToObj(index -> """
                        {"source":"jumpball","title":"Article %d","url":"https://jumpball.test/%d","publishedAt":"2026-01-01T12:00:00"}
                        """.formatted(index, index).trim())
                .collect(Collectors.joining(","));
        return "{\"articles\":[" + articles + "]}";
    }

    private Schedule schedule() {
        return Schedule.builder()
                .id("schedule-1")
                .season("2025-2026")
                .startDateTime(LocalDateTime.of(2026, 1, 2, 19, 0))
                .opponent("KB스타즈")
                .isHome(true)
                .extraHome("사직")
                .specialGame(false)
                .isActive(true)
                .build();
    }
}
