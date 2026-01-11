package com.supersohee.api.article.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import com.supersohee.api.article.repository.ArticleRepository;
import com.supersohee.api.article.domain.Article;
import com.supersohee.api.article.dto.ArticlePageResponse;
import lombok.RequiredArgsConstructor;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ArticleService {
    private final ArticleRepository articleRepository;
    
    // 메인 페이지용: 가장 최근 기사 1개 (소스 상관없이)
    public Optional<Article> getLatestArticle() {
        return articleRepository.findFirstByOrderByPublishedAtDesc();
    }
    
    // 점프볼/루키별 기사 (페이지네이션 + 메타 정보 포함)
    public ArticlePageResponse getBySource(String source, int page, int limit) {
        Pageable pageable = PageRequest.of(page, limit);
        Page<Article> articlePage = articleRepository.findBySourceOrderByPublishedAtDesc(source, pageable);
        
        return ArticlePageResponse.builder()
                .articles(articlePage.getContent())
                .total(articlePage.getTotalElements())
                .page(page)
                .limit(limit)
                .totalPages(articlePage.getTotalPages())
                .hasNext(articlePage.hasNext())
                .hasPrevious(articlePage.hasPrevious())
                .build();
    }
}

