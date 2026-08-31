package com.supersohee.api.article.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public record AdminArticleImportItem(
        @NotBlank @Pattern(regexp = "jumpball|rookie") String source,
        @NotBlank @Size(max = 300) String title,
        @NotBlank @Size(max = 2_000) String url,
        @Size(max = 20_000) String summary,
        @Size(max = 2_000) String imageUrl,
        @NotNull LocalDateTime publishedAt) {
}
