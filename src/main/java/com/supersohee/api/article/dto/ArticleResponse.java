package com.supersohee.api.article.dto;

import com.supersohee.api.article.domain.Article;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public record ArticleResponse(
        String id,
        String source,
        String title,
        String url,
        String summary,
        String imageUrl,
        String publishedAt,
        String crawledAt,
        Integer score,
        Boolean mainTarget) {
    private static final ZoneId SEOUL_ZONE = ZoneId.of("Asia/Seoul");

    public static ArticleResponse from(Article article) {
        return new ArticleResponse(
                article.getId(),
                article.getSource(),
                article.getTitle(),
                article.getUrl(),
                article.getSummary(),
                article.getImageUrl(),
                withSeoulOffset(article.getPublishedAt()),
                withSeoulOffset(article.getCrawledAt()),
                article.getScore(),
                article.getMainTarget());
    }

    private static String withSeoulOffset(LocalDateTime value) {
        return value == null
                ? null
                : value.atZone(SEOUL_ZONE).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
    }
}
