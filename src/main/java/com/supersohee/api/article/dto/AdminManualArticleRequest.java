package com.supersohee.api.article.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AdminManualArticleRequest(
        @NotBlank @Size(max = 300) String title,
        @NotBlank @Size(max = 20_000) String content) {
}
