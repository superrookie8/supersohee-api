package com.supersohee.api.image.admin.dto;

import java.util.List;

public record AdminPhotoUploadResponse(String message, List<AdminPhotoItemResponse> photos) {
}
