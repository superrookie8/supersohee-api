package com.supersohee.api.article.controller;

import com.supersohee.api.article.service.ArticleService;
import com.supersohee.api.article.domain.Article;
import com.supersohee.api.article.dto.ArticlePageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;

@RestController
@RequestMapping("/api/articles")
@RequiredArgsConstructor
public class ArticleController {

    private final ArticleService articleService;

    // 메인 페이지용: 가장 최근 기사 1개 (소스 상관없이)
    @GetMapping("/latest")
    public ResponseEntity<Article> getLatestArticle() {
        return articleService.getLatestArticle()
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    // 점프볼/루키별 기사 모음 (페이지네이션 + 메타 정보)
    @GetMapping("/{source}")
    public ResponseEntity<ArticlePageResponse> getArticlesBySource(
        @PathVariable String source,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "10") int limit) {
        ArticlePageResponse response = articleService.getBySource(source, page, limit);
        return ResponseEntity.ok(response);
    }
}
