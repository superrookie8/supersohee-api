package com.supersohee.api.article.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import com.supersohee.api.article.repository.ArticleRepository;
import com.supersohee.api.article.domain.Article;
import com.supersohee.api.article.dto.ArticlePageResponse;
import com.supersohee.api.article.dto.ArticleResponse;
import com.supersohee.api.article.dto.AdminArticleImportItem;
import com.supersohee.api.article.dto.AdminArticleImportRequest;
import com.supersohee.api.article.dto.AdminArticleImportResponse;
import com.supersohee.api.article.dto.AdminManualArticleRequest;
import lombok.RequiredArgsConstructor;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ArticleService {
    private final ArticleRepository articleRepository;
    private final MongoOperations mongoOperations;
    
    // 메인 페이지용: 가장 최근 기사 1개 (소스 상관없이)
    public Optional<Article> getLatestArticle() {
        return articleRepository.findFirstByOrderByPublishedAtDesc();
    }
    
    // 점프볼/루키별 기사 (페이지네이션 + 메타 정보 포함)
    public ArticlePageResponse getBySource(String source, int page, int limit) {
        Pageable pageable = PageRequest.of(page, limit);
        Page<Article> articlePage = articleRepository.findBySourceOrderByPublishedAtDesc(source, pageable);
        
        return ArticlePageResponse.builder()
                .articles(articlePage.getContent().stream().map(ArticleResponse::from).toList())
                .total(articlePage.getTotalElements())
                .page(page)
                .limit(limit)
                .totalPages(articlePage.getTotalPages())
                .hasNext(articlePage.hasNext())
                .hasPrevious(articlePage.hasPrevious())
                .build();
    }

    public Page<Article> getAdminArticles(String source, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        if (source == null || source.isBlank()) {
            return articleRepository.findAllByOrderByPublishedAtDesc(pageable);
        }
        return articleRepository.findBySourceOrderByPublishedAtDesc(source.trim().toLowerCase(), pageable);
    }

    public Article createManualArticle(AdminManualArticleRequest request) {
        LocalDateTime now = LocalDateTime.now();
        return articleRepository.save(Article.builder()
                .source("manual")
                .title(request.title().trim())
                .summary(request.content().trim())
                .publishedAt(now)
                .crawledAt(now)
                .build());
    }

    public AdminArticleImportResponse importArticles(AdminArticleImportRequest request) {
        int created = 0;
        int existing = 0;
        for (AdminArticleImportItem item : request.articles()) {
            String source = item.source().toLowerCase();
            Query identity = Query.query(Criteria.where("source").is(source).and("url").is(item.url()));
            Update insertOnly = new Update()
                    .setOnInsert("source", source)
                    .setOnInsert("title", item.title().trim())
                    .setOnInsert("url", item.url())
                    .setOnInsert("summary", item.summary())
                    .setOnInsert("imageUrl", item.imageUrl())
                    .setOnInsert("publishedAt", item.publishedAt())
                    .setOnInsert("crawledAt", LocalDateTime.now());
            try {
                var result = mongoOperations.upsert(identity, insertOnly, Article.class);
                if (result.getUpsertedId() != null) {
                    created++;
                } else {
                    existing++;
                }
            } catch (DuplicateKeyException duplicate) {
                // A competing atomic upsert may win between server-side match
                // and insert. Count only the same identity as existing.
                if (mongoOperations.exists(identity, Article.class)) {
                    existing++;
                } else {
                    throw duplicate;
                }
            }
        }
        return new AdminArticleImportResponse(request.articles().size(), created, existing);
    }
}
