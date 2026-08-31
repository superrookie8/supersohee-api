package com.supersohee.api.article.dto;

import com.supersohee.api.article.domain.Article;

import java.time.LocalDateTime;

public record AdminArticleResponse(
        String id,
        String source,
        String title,
        String url,
        String summary,
        String imageUrl,
        LocalDateTime publishedAt,
        LocalDateTime crawledAt) {
    public static AdminArticleResponse from(Article article) {
        return new AdminArticleResponse(
                article.getId(), article.getSource(), article.getTitle(), article.getUrl(),
                article.getSummary(), article.getImageUrl(), article.getPublishedAt(), article.getCrawledAt());
    }
}
