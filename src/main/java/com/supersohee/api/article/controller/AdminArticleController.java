package com.supersohee.api.article.controller;

import com.supersohee.api.admin.error.AdminApiException;
import com.supersohee.api.article.dto.*;
import com.supersohee.api.article.service.ArticleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/articles")
@RequiredArgsConstructor
public class AdminArticleController {
    private static final int MAX_PAGE_SIZE = 100;
    private final ArticleService articleService;

    @GetMapping
    public AdminArticlePageResponse getArticles(
            @RequestParam(required = false) String source,
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        if (page < 0 || size < 1 || size > MAX_PAGE_SIZE) {
            throw AdminApiException.badRequest("page must be non-negative and size must be between 1 and 100.");
        }
        if (q != null && q.length() > 100) {
            throw AdminApiException.badRequest("q must be at most 100 characters.");
        }
        return AdminArticlePageResponse.from(q == null || q.isBlank()
                ? articleService.getAdminArticles(source, page, size)
                : articleService.searchAdminArticles(source, q, page, size));
    }

    @PostMapping
    public ResponseEntity<AdminArticleResponse> createManualArticle(
            @Valid @RequestBody AdminManualArticleRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(AdminArticleResponse.from(articleService.createManualArticle(request)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteArticle(@PathVariable String id) {
        articleService.deleteAdminArticle(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/batch")
    public AdminArticleImportResponse batchArticles(@Valid @RequestBody AdminArticleImportRequest request) {
        return articleService.batchArticles(request);
    }

    @PostMapping("/import")
    public AdminArticleImportResponse importArticles(@Valid @RequestBody AdminArticleImportRequest request) {
        return articleService.importArticles(request);
    }
}
