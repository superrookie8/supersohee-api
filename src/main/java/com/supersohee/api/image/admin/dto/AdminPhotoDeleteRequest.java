package com.supersohee.api.image.admin.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

public record AdminPhotoDeleteRequest(
        @NotEmpty(message = "photoIds must not be empty")
        @Size(max = 100, message = "photoIds must contain at most 100 items")
        List<@NotBlank(message = "photo id must not be blank") String> photoIds) {
}
