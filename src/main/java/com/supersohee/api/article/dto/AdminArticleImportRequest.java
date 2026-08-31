package com.supersohee.api.article.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

public record AdminArticleImportRequest(
        @NotEmpty @Size(max = 200) List<@Valid AdminArticleImportItem> articles) {
}
