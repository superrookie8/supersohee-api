package com.supersohee.api.image.admin.dto;

import java.util.List;

public record AdminPhotoListResponse(
        List<AdminPhotoItemResponse> adminPhotos,
        List<AdminPhotoItemResponse> userPhotos) {
}
