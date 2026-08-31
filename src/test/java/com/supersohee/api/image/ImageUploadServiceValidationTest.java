package com.supersohee.api.image.service;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

class ImageUploadServiceValidationTest {
    private final ImageUploadService service = new ImageUploadService();

    @Test
    void eventPhotoOverFiveMibIsRejectedBeforeR2Access() {
        MockMultipartFile oversized = new MockMultipartFile(
                "photos", "large.jpg", "image/jpeg", new byte[5 * 1024 * 1024 + 1]);

        assertReason(ImageValidationException.Reason.TOO_LARGE, () -> service.uploadEventImage(oversized));
    }

    @Test
    void unsupportedEventPhotoExtensionIsRejectedBeforeR2Access() {
        MockMultipartFile unsupported = new MockMultipartFile(
                "photos", "photo.heic", "image/heic", new byte[]{1});

        assertReason(ImageValidationException.Reason.UNSUPPORTED_FORMAT,
                () -> service.uploadEventImage(unsupported));
    }

    @Test
    void mismatchedMimeAndTraversalFilenameAreRejectedBeforeR2Access() {
        MockMultipartFile mismatched = new MockMultipartFile(
                "photos", "photo.png", "image/jpeg", new byte[]{1});
        MockMultipartFile traversal = new MockMultipartFile(
                "photos", "../photo.png", "image/png", new byte[]{1});

        assertReason(ImageValidationException.Reason.UNSUPPORTED_FORMAT,
                () -> service.uploadEventImage(mismatched));
        assertReason(ImageValidationException.Reason.INVALID_FILENAME,
                () -> service.uploadEventImage(traversal));
    }

    @Test
    void browserCompressedWebpContractIsAcceptedBeforeR2Access() {
        MockMultipartFile webp = new MockMultipartFile(
                "photos", "event.webp", "image/webp",
                new byte[]{'R', 'I', 'F', 'F', 1, 2, 3, 4, 'W', 'E', 'B', 'P'});
        MockMultipartFile spoofed = new MockMultipartFile(
                "photos", "event.webp", "image/webp", new byte[]{1, 2, 3});

        service.validateImageForUpload(webp);
        assertReason(ImageValidationException.Reason.UNSUPPORTED_FORMAT,
                () -> service.validateImageForUpload(spoofed));
    }

    @Test
    void batchPrevalidatesEveryPhotoBeforeStartingAnyR2Upload() throws Exception {
        ImageUploadService batchService = spy(new ImageUploadService());
        MockMultipartFile valid = new MockMultipartFile(
                "photos", "valid.png", "image/png", new byte[]{1});
        MockMultipartFile spoofedWebp = new MockMultipartFile(
                "photos", "spoofed.webp", "image/webp", new byte[]{1, 2, 3});

        assertReason(ImageValidationException.Reason.UNSUPPORTED_FORMAT,
                () -> batchService.uploadEventImages(List.of(valid, spoofedWebp)));

        verify(batchService, never()).uploadEventImage(valid);
        verify(batchService, never()).uploadEventImage(spoofedWebp);
    }

    @Test
    void partialBatchUploadDeletesOnlyKeysCreatedByThatBatch() throws Exception {
        ImageUploadService batchService = spy(new ImageUploadService());
        MockMultipartFile first = new MockMultipartFile(
                "photos", "first.png", "image/png", new byte[]{1});
        MockMultipartFile second = new MockMultipartFile(
                "photos", "second.png", "image/png", new byte[]{2});
        doReturn("event/new-first.png").when(batchService).uploadEventImage(first);
        doThrow(new IOException("fixture upload failure")).when(batchService).uploadEventImage(second);

        assertThatExceptionOfType(IOException.class)
                .isThrownBy(() -> batchService.uploadEventImages(List.of(first, second)));

        verify(batchService).deleteImage("event/new-first.png");
    }

    private void assertReason(ImageValidationException.Reason reason, ThrowingOperation operation) {
        assertThatExceptionOfType(ImageValidationException.class)
                .isThrownBy(operation::run)
                .satisfies(exception -> assertThat(exception.reason()).isEqualTo(reason));
    }

    @FunctionalInterface
    private interface ThrowingOperation {
        void run() throws Exception;
    }
}
