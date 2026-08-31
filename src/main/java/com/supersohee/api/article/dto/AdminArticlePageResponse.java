package com.supersohee.api.article.dto;

import com.supersohee.api.article.domain.Article;
import org.springframework.data.domain.Page;

import java.util.List;

public record AdminArticlePageResponse(
        List<AdminArticleResponse> content,
        long totalElements,
        int page,
        int size,
        int totalPages,
        boolean hasNext) {
    public static AdminArticlePageResponse from(Page<Article> result) {
        return new AdminArticlePageResponse(
                result.getContent().stream().map(AdminArticleResponse::from).toList(),
                result.getTotalElements(), result.getNumber(), result.getSize(),
                result.getTotalPages(), result.hasNext());
    }
}
