package com.supersohee.api.guestbook.dto;

import com.supersohee.api.guestbook.domain.Guestbook;

import java.time.LocalDateTime;

public record AdminGuestbookResponse(
        String id,
        String name,
        String message,
        LocalDateTime date,
        String photoId,
        boolean hasPhoto) {
    public static AdminGuestbookResponse from(Guestbook guestbook) {
        String photoId = guestbook.effectivePhotoId();
        return new AdminGuestbookResponse(
                guestbook.getId(), guestbook.getName(), guestbook.getMessage(),
                guestbook.effectiveDate(), photoId, photoId != null && !photoId.isBlank());
    }
}
