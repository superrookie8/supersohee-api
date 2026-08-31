package com.supersohee.api.guestbook.service;

import com.supersohee.api.admin.error.AdminApiException;
import com.supersohee.api.guestbook.domain.Guestbook;
import com.supersohee.api.guestbook.repository.GuestbookPhotoStore;
import com.supersohee.api.guestbook.repository.GuestbookRepository;
import lombok.RequiredArgsConstructor;
import org.bson.types.ObjectId;
import org.springframework.http.InvalidMediaTypeException;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;

import java.util.Set;

@Service
@RequiredArgsConstructor
public class GuestbookPhotoService {
    static final long MAX_PHOTO_BYTES = 5L * 1024 * 1024;
    private static final Set<String> ALLOWED_MEDIA_TYPES = Set.of(
            MediaType.IMAGE_JPEG_VALUE,
            MediaType.IMAGE_PNG_VALUE,
            MediaType.IMAGE_GIF_VALUE,
            "image/webp",
            "image/jpg"
    );

    private final GuestbookRepository guestbookRepository;
    private final GuestbookPhotoStore photoStore;

    public GuestbookPhoto getPhotoForEntry(String guestbookId) {
        Guestbook guestbook = guestbookRepository.findById(guestbookId)
                .orElseThrow(() -> AdminApiException.notFound("Guestbook entry"));

        ObjectId photoId = parseLinkedPhotoId(guestbook.effectivePhotoId());
        GuestbookPhotoStore.StoredGuestbookPhoto storedPhoto = photoStore.findById(photoId)
                .orElseThrow(() -> AdminApiException.notFound("Guestbook photo"));

        if (storedPhoto.contentLength() < 0 || storedPhoto.contentLength() > MAX_PHOTO_BYTES) {
            throw AdminApiException.payloadTooLarge("Guestbook photo exceeds the 5 MiB limit.");
        }

        MediaType mediaType = parseAllowedMediaType(storedPhoto.contentType());
        return new GuestbookPhoto(storedPhoto.resource(), mediaType, storedPhoto.contentLength());
    }

    private ObjectId parseLinkedPhotoId(String rawPhotoId) {
        if (rawPhotoId == null || rawPhotoId.isBlank() || !ObjectId.isValid(rawPhotoId)) {
            throw AdminApiException.notFound("Guestbook photo");
        }
        return new ObjectId(rawPhotoId);
    }

    private MediaType parseAllowedMediaType(String rawContentType) {
        try {
            MediaType mediaType = MediaType.parseMediaType(rawContentType == null ? "" : rawContentType);
            String normalized = mediaType.getType().toLowerCase() + "/" + mediaType.getSubtype().toLowerCase();
            if (!ALLOWED_MEDIA_TYPES.contains(normalized)) {
                throw AdminApiException.unsupportedMediaType("Guestbook photo content type is not supported.");
            }
            return mediaType;
        } catch (InvalidMediaTypeException exception) {
            throw AdminApiException.unsupportedMediaType("Guestbook photo content type is not supported.");
        }
    }
}
