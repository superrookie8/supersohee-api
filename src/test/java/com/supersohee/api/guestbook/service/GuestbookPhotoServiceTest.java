package com.supersohee.api.guestbook.service;

import com.supersohee.api.admin.error.AdminApiException;
import com.supersohee.api.guestbook.domain.Guestbook;
import com.supersohee.api.guestbook.repository.GuestbookPhotoStore;
import com.supersohee.api.guestbook.repository.GuestbookRepository;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class GuestbookPhotoServiceTest {
    private static final String PHOTO_ID = "507f1f77bcf86cd799439011";

    private final GuestbookRepository guestbookRepository = mock(GuestbookRepository.class);
    private final GuestbookPhotoStore photoStore = mock(GuestbookPhotoStore.class);
    private final GuestbookPhotoService service = new GuestbookPhotoService(guestbookRepository, photoStore);

    @Test
    void resolvesOnlyThePhotoLinkedFromTheGuestbookEntry() {
        byte[] bytes = {1, 2, 3};
        when(guestbookRepository.findById("guest-1")).thenReturn(Optional.of(entry(PHOTO_ID)));
        when(photoStore.findById(new ObjectId(PHOTO_ID))).thenReturn(Optional.of(
                new GuestbookPhotoStore.StoredGuestbookPhoto(
                        new ByteArrayResource(bytes), MediaType.IMAGE_PNG_VALUE, bytes.length)));

        GuestbookPhoto result = service.getPhotoForEntry("guest-1");

        assertThat(result.mediaType()).isEqualTo(MediaType.IMAGE_PNG);
        assertThat(result.contentLength()).isEqualTo(bytes.length);
        verify(photoStore).findById(new ObjectId(PHOTO_ID));
    }

    @Test
    void malformedLegacyPhotoIdCannotReachTheBackingStore() {
        when(guestbookRepository.findById("guest-1")).thenReturn(Optional.of(entry("../../etc/passwd")));

        assertStatus(HttpStatus.NOT_FOUND, () -> service.getPhotoForEntry("guest-1"));

        verifyNoInteractions(photoStore);
    }

    @Test
    void missingEntryOrLinkedObjectReturnsNotFound() {
        when(guestbookRepository.findById("missing")).thenReturn(Optional.empty());
        assertStatus(HttpStatus.NOT_FOUND, () -> service.getPhotoForEntry("missing"));

        when(guestbookRepository.findById("guest-1")).thenReturn(Optional.of(entry(PHOTO_ID)));
        when(photoStore.findById(new ObjectId(PHOTO_ID))).thenReturn(Optional.empty());
        assertStatus(HttpStatus.NOT_FOUND, () -> service.getPhotoForEntry("guest-1"));
    }

    @Test
    void oversizedPhotoIsRejectedBeforeStreaming() {
        when(guestbookRepository.findById("guest-1")).thenReturn(Optional.of(entry(PHOTO_ID)));
        when(photoStore.findById(new ObjectId(PHOTO_ID))).thenReturn(Optional.of(
                new GuestbookPhotoStore.StoredGuestbookPhoto(
                        new ByteArrayResource(new byte[0]), MediaType.IMAGE_JPEG_VALUE,
                        GuestbookPhotoService.MAX_PHOTO_BYTES + 1)));

        assertStatus(HttpStatus.PAYLOAD_TOO_LARGE, () -> service.getPhotoForEntry("guest-1"));
    }

    @Test
    void unsafeOrMissingContentTypeIsRejected() {
        when(guestbookRepository.findById("guest-1")).thenReturn(Optional.of(entry(PHOTO_ID)));
        when(photoStore.findById(new ObjectId(PHOTO_ID))).thenReturn(Optional.of(
                new GuestbookPhotoStore.StoredGuestbookPhoto(
                        new ByteArrayResource(new byte[]{1}), "image/svg+xml", 1)));

        assertStatus(HttpStatus.UNSUPPORTED_MEDIA_TYPE, () -> service.getPhotoForEntry("guest-1"));
    }

    private Guestbook entry(String photoId) {
        return Guestbook.builder().id("guest-1").legacyPhotoId(photoId).build();
    }

    private void assertStatus(HttpStatus expected, Runnable operation) {
        assertThatExceptionOfType(AdminApiException.class)
                .isThrownBy(operation::run)
                .satisfies(exception -> assertThat(exception.status()).isEqualTo(expected));
    }
}
