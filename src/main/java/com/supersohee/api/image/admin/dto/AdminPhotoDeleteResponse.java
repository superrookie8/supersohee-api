package com.supersohee.api.image.admin.dto;

import java.util.List;

public record AdminPhotoDeleteResponse(
        String message,
        List<String> deletedIds,
        List<String> failedIds) {

    public boolean complete() {
        return failedIds.isEmpty();
    }
}
