package com.supersohee.api.article;

import com.supersohee.api.article.domain.Article;
import com.supersohee.api.article.dto.ArticlePageResponse;
import com.supersohee.api.article.dto.ArticleResponse;
import com.supersohee.api.article.service.ArticleService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ArticlePublicContractIntegrationTest {
    @Autowired MockMvc mockMvc;
    @MockitoBean ArticleService articleService;

    @Test
    void sourceIsNormalizedAndLatestPageShapeUsesSeoulOffsetDatetime() throws Exception {
        Article latest = Article.builder()
                .id("article-1")
                .source("jumpball")
                .title("Latest")
                .url("https://jumpball.test/1")
                .publishedAt(LocalDateTime.of(2026, 1, 23, 10, 30, 0, 123_000_000))
                .build();
        ArticlePageResponse response = ArticlePageResponse.builder()
                .articles(List.of(ArticleResponse.from(latest)))
                .total(1)
                .page(0)
                .limit(1)
                .totalPages(1)
                .hasNext(false)
                .hasPrevious(false)
                .build();
        when(articleService.getBySource("jumpball", 0, 1)).thenReturn(response);

        mockMvc.perform(get("/api/articles/JumpBall").queryParam("page", "0").queryParam("limit", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.articles[0].id").value("article-1"))
                .andExpect(jsonPath("$.articles[0].source").value("jumpball"))
                .andExpect(jsonPath("$.articles[0].publishedAt")
                        .value("2026-01-23T10:30:00.123+09:00"))
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.limit").value(1))
                .andExpect(jsonPath("$.totalPages").value(1))
                .andExpect(jsonPath("$.hasNext").value(false))
                .andExpect(jsonPath("$.hasPrevious").value(false));
        verify(articleService).getBySource("jumpball", 0, 1);
    }

    @Test
    void unsupportedSourceAndPaginationBoundsReturnSafeFieldErrors() throws Exception {
        mockMvc.perform(get("/api/articles/unknown").queryParam("page", "-1").queryParam("limit", "101"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("ARTICLE_VALIDATION_FAILED"))
                .andExpect(jsonPath("$.message").value("Article request validation failed."))
                .andExpect(jsonPath("$.traceId").isNotEmpty())
                .andExpect(jsonPath("$.fieldErrors.source").value("source must be jumpball or rookie."))
                .andExpect(jsonPath("$.fieldErrors.page").value("page must be non-negative."))
                .andExpect(jsonPath("$.fieldErrors.limit").value("limit must be between 1 and 100."));

        mockMvc.perform(get("/api/articles/jumpball").queryParam("page", "not-an-integer"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.page").value("page must be an integer."));
    }
}
