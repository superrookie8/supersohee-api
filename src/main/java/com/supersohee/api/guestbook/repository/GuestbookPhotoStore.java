package com.supersohee.api.guestbook.repository;

import org.bson.types.ObjectId;
import org.springframework.core.io.Resource;

import java.util.Optional;

public interface GuestbookPhotoStore {
    Optional<StoredGuestbookPhoto> findById(ObjectId photoId);

    record StoredGuestbookPhoto(Resource resource, String contentType, long contentLength) {
    }
}
