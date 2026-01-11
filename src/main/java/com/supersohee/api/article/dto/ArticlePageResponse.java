package com.supersohee.api.article.dto;

import com.supersohee.api.article.domain.Article;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ArticlePageResponse {
    private List<Article> articles;
    private long total;           // 전체 기사 개수
    private int page;             // 현재 페이지 (0부터 시작)
    private int limit;            // 페이지당 개수
    private int totalPages;       // 전체 페이지 수
    private boolean hasNext;      // 다음 페이지 존재 여부
    private boolean hasPrevious; // 이전 페이지 존재 여부
}

