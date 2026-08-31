package com.supersohee.api.image.admin;

import com.supersohee.api.admin.error.AdminApiException;
import com.supersohee.api.image.admin.domain.AdminPhotoSource;
import com.supersohee.api.image.admin.domain.StoredAdminPhoto;
import com.supersohee.api.image.admin.dto.AdminPhotoDeleteResponse;
import com.supersohee.api.image.admin.repository.AdminPhotoStore;
import com.supersohee.api.image.admin.service.AdminPhotoContent;
import com.supersohee.api.image.admin.service.AdminPhotoService;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AdminPhotoServiceTest {
    private static final String FIRST_ID = "507f1f77bcf86cd799439011";
    private static final String SECOND_ID = "507f191e810c19729de860ea";

    private final AdminPhotoStore photoStore = mock(AdminPhotoStore.class);
    private final AdminPhotoService service = new AdminPhotoService(photoStore);

    @Test
    void listsLegacyAdminAndUserBucketsWithoutEmbeddingBytes() {
        when(photoStore.findAll(AdminPhotoSource.ADMIN)).thenReturn(List.of(photo(FIRST_ID, AdminPhotoSource.ADMIN)));
        when(photoStore.findAll(AdminPhotoSource.USER)).thenReturn(List.of(photo(SECOND_ID, AdminPhotoSource.USER)));

        var response = service.findAll();

        assertThat(response.adminPhotos()).extracting(item -> item.id()).containsExactly(FIRST_ID);
        assertThat(response.userPhotos()).extracting(item -> item.id()).containsExactly(SECOND_ID);
    }

    @Test
    void validatesExtensionMimeSignatureAndFilenameBeforeStorage() throws IOException {
        MockMultipartFile spoofed = new MockMultipartFile(
                "photos", "photo.png", MediaType.IMAGE_PNG_VALUE, new byte[]{1, 2, 3});
        assertStatus(HttpStatus.UNSUPPORTED_MEDIA_TYPE, () -> service.upload(List.of(spoofed)));

        MockMultipartFile mismatch = new MockMultipartFile(
                "photos", "photo.jpg", MediaType.IMAGE_PNG_VALUE, pngBytes());
        assertStatus(HttpStatus.UNSUPPORTED_MEDIA_TYPE, () -> service.upload(List.of(mismatch)));

        MockMultipartFile traversal = new MockMultipartFile(
                "photos", "../photo.png", MediaType.IMAGE_PNG_VALUE, pngBytes());
        assertStatus(HttpStatus.BAD_REQUEST, () -> service.upload(List.of(traversal)));

        MockMultipartFile oversized = new MockMultipartFile(
                "photos", "large.png", MediaType.IMAGE_PNG_VALUE, new byte[5 * 1024 * 1024 + 1]);
        assertStatus(HttpStatus.PAYLOAD_TOO_LARGE, () -> service.upload(List.of(oversized)));

        verify(photoStore, never()).storeAdminPhoto(spoofed);
        verify(photoStore, never()).storeAdminPhoto(mismatch);
        verify(photoStore, never()).storeAdminPhoto(traversal);
        verify(photoStore, never()).storeAdminPhoto(oversized);
    }

    @Test
    void failedBatchUploadRollsBackAlreadyStoredPhotos() throws IOException {
        MockMultipartFile first = png("first.png");
        MockMultipartFile second = png("second.png");
        StoredAdminPhoto stored = photo(FIRST_ID, AdminPhotoSource.ADMIN);
        when(photoStore.storeAdminPhoto(first)).thenReturn(stored);
        when(photoStore.storeAdminPhoto(second)).thenThrow(new IOException("storage unavailable"));

        assertStatus(HttpStatus.BAD_GATEWAY, () -> service.upload(List.of(first, second)));

        verify(photoStore).delete(stored);
    }

    @Test
    void browserCompressedWebpUsesTheSameMagicContractAsEventUploads() throws IOException {
        MockMultipartFile webp = new MockMultipartFile(
                "photos", "gallery.webp", "image/webp",
                new byte[]{'R', 'I', 'F', 'F', 1, 2, 3, 4, 'W', 'E', 'B', 'P'});
        StoredAdminPhoto stored = new StoredAdminPhoto(
                FIRST_ID, AdminPhotoSource.ADMIN, "gallery.webp", "image/webp",
                webp.getSize(), Instant.EPOCH, null);
        when(photoStore.storeAdminPhoto(webp)).thenReturn(stored);

        var response = service.upload(List.of(webp));

        assertThat(response.photos()).singleElement()
                .satisfies(item -> assertThat(item.contentType()).isEqualTo("image/webp"));
    }

    @Test
    void deletePreflightsEveryDeduplicatedBoundIdBeforeMutation() {
        StoredAdminPhoto first = photo(FIRST_ID, AdminPhotoSource.ADMIN);
        when(photoStore.findByIdAdminFirst(FIRST_ID)).thenReturn(Optional.of(first));
        when(photoStore.findByIdAdminFirst(SECOND_ID)).thenReturn(Optional.empty());

        assertStatus(HttpStatus.NOT_FOUND, () -> service.delete(List.of(FIRST_ID, FIRST_ID, SECOND_ID)));

        verify(photoStore, never()).delete(first);
    }

    @Test
    void malformedIdCannotReachStorageAndPartialDeleteIsReported() {
        assertStatus(HttpStatus.BAD_REQUEST, () -> service.delete(List.of("../../event/photo.png")));
        verify(photoStore, never()).findByIdAdminFirst("../../event/photo.png");

        StoredAdminPhoto first = photo(FIRST_ID, AdminPhotoSource.ADMIN);
        StoredAdminPhoto second = photo(SECOND_ID, AdminPhotoSource.USER);
        when(photoStore.findByIdAdminFirst(FIRST_ID)).thenReturn(Optional.of(first));
        when(photoStore.findByIdAdminFirst(SECOND_ID)).thenReturn(Optional.of(second));
        org.mockito.Mockito.doThrow(new IllegalStateException("storage unavailable"))
                .when(photoStore).delete(second);

        AdminPhotoDeleteResponse response = service.delete(List.of(FIRST_ID, SECOND_ID));

        assertThat(response.complete()).isFalse();
        assertThat(response.deletedIds()).containsExactly(FIRST_ID);
        assertThat(response.failedIds()).containsExactly(SECOND_ID);
    }

    @Test
    void protectedContentKeepsStoredMimeAndAppliesSizeLimit() {
        StoredAdminPhoto photo = photo(FIRST_ID, AdminPhotoSource.ADMIN);
        when(photoStore.findByIdAdminFirst(FIRST_ID)).thenReturn(Optional.of(photo));

        AdminPhotoContent content = service.getContent(FIRST_ID);

        assertThat(content.mediaType()).isEqualTo(MediaType.IMAGE_PNG);
        assertThat(content.contentLength()).isEqualTo(pngBytes().length);

        StoredAdminPhoto oversized = new StoredAdminPhoto(
                SECOND_ID, AdminPhotoSource.USER, "large.png", MediaType.IMAGE_PNG_VALUE,
                5L * 1024 * 1024 + 1, Instant.EPOCH, new ByteArrayResource(new byte[0]));
        when(photoStore.findByIdAdminFirst(SECOND_ID)).thenReturn(Optional.of(oversized));
        assertStatus(HttpStatus.PAYLOAD_TOO_LARGE, () -> service.getContent(SECOND_ID));
    }

    private StoredAdminPhoto photo(String id, AdminPhotoSource source) {
        byte[] bytes = pngBytes();
        return new StoredAdminPhoto(
                id, source, "photo.png", MediaType.IMAGE_PNG_VALUE,
                bytes.length, Instant.parse("2026-01-01T00:00:00Z"), new ByteArrayResource(bytes));
    }

    private MockMultipartFile png(String filename) {
        return new MockMultipartFile("photos", filename, MediaType.IMAGE_PNG_VALUE, pngBytes());
    }

    private byte[] pngBytes() {
        return new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 1};
    }

    private void assertStatus(HttpStatus status, Runnable operation) {
        assertThatExceptionOfType(AdminApiException.class)
                .isThrownBy(operation::run)
                .satisfies(exception -> assertThat(exception.status()).isEqualTo(status));
    }
}
