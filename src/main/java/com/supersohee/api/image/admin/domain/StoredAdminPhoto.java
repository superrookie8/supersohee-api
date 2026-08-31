package com.supersohee.api.image.admin.domain;

import org.springframework.core.io.Resource;

import java.time.Instant;

public record StoredAdminPhoto(
        String id,
        AdminPhotoSource source,
        String filename,
        String contentType,
        long size,
        Instant uploadedAt,
        Resource resource) {
}
