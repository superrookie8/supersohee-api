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
import com.supersohee.api.admin.error.AdminApiException;
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
        Pageable pageable = PageRequest.of(page, limit, org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "id"));
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

    public ArticlePageResponse getPage(String source, int page, int limit) {
        var sources = source == null || "all".equals(source) ? java.util.List.of("jumpball", "rookie", "other") : java.util.List.of(source);
        var result = articleRepository.findBySourceIn(sources, PageRequest.of(page, limit,
                org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "publishedAt", "id")));
        return ArticlePageResponse.builder().articles(result.getContent().stream().map(ArticleResponse::from).toList())
                .total(result.getTotalElements()).page(page).limit(limit).totalPages(result.getTotalPages())
                .hasNext(result.hasNext()).hasPrevious(result.hasPrevious()).build();
    }

    public Page<Article> getAdminArticles(String source, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        if (source == null || source.isBlank()) {
            return articleRepository.findAllByOrderByPublishedAtDesc(pageable);
        }
        return articleRepository.findBySourceOrderByPublishedAtDesc(source.trim().toLowerCase(), pageable);
    }

    // 관리자 제목 검색: 입력값은 정규식 문자가 아닌 글자 그대로 찾는다.
    public Page<Article> searchAdminArticles(String source, String q, int page, int size) {
        Criteria criteria = Criteria.where("title").regex(java.util.regex.Pattern.quote(q.trim()), "i");
        if (source != null && !source.isBlank()) {
            criteria = criteria.and("source").is(source.trim().toLowerCase());
        }
        Pageable pageable = PageRequest.of(page, size,
                org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "publishedAt", "id"));
        long total = mongoOperations.count(new Query(criteria), Article.class);
        java.util.List<Article> content = mongoOperations.find(new Query(criteria).with(pageable), Article.class);
        return new org.springframework.data.domain.PageImpl<>(content, pageable, total);
    }

    // 관리자 삭제: 지정한 기사 1건만 지운다. 같은 URL이 다시 import되면 새 기사로 들어온다.
    public void deleteAdminArticle(String id) {
        if (id == null || !id.matches("[0-9a-f]{24}")) {
            throw AdminApiException.badRequest("Article id must be a 24-character hexadecimal identifier.");
        }
        if (!articleRepository.existsById(id)) {
            throw AdminApiException.notFound("Article");
        }
        articleRepository.deleteById(id);
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

    public AdminArticleImportResponse batchArticles(AdminArticleImportRequest request) {
        // Validate the entire request before the first write. Storage failure may still
        // leave a partial batch; insert-only retries safely resume those writes.
        var normalized = request.articles().stream().map(ArticleUrlPolicy::normalize).toList();
        int existing = 0;
        int created = 0;
        for (var item : normalized) {
            Query legacy = Query.query(Criteria.where("source").is(item.source())
                    .and("url").regex(ArticleUrlPolicy.legacyPattern(item.url())));
            if (mongoOperations.exists(legacy, Article.class)) {
                existing++;
                continue;
            }
            var result = persistArticles(new AdminArticleImportRequest(java.util.List.of(item)));
            existing += result.existing();
            created += result.created();
        }
        return new AdminArticleImportResponse(normalized.size(), created, existing);
    }

    public AdminArticleImportResponse importArticles(AdminArticleImportRequest request) {
        return batchArticles(request);
    }

    private AdminArticleImportResponse persistArticles(AdminArticleImportRequest request) {
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
