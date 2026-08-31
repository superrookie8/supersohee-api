package com.supersohee.api.image.admin.dto;

import com.supersohee.api.image.admin.domain.StoredAdminPhoto;

import java.time.Instant;

public record AdminPhotoItemResponse(
        String id,
        String filename,
        String contentType,
        long size,
        Instant uploadedAt) {

    public static AdminPhotoItemResponse from(StoredAdminPhoto photo) {
        return new AdminPhotoItemResponse(
                photo.id(), photo.filename(), photo.contentType(), photo.size(), photo.uploadedAt());
    }
}
