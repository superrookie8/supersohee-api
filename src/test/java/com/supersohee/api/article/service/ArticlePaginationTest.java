package com.supersohee.api.article.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.supersohee.api.article.domain.Article;
import com.supersohee.api.article.repository.ArticleRepository;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.*;
import org.springframework.data.mongodb.core.MongoOperations;

import java.time.LocalDateTime;
import java.util.*;

class ArticlePaginationTest {
    private final ArticleRepository repository = mock(ArticleRepository.class);
    private final ArticleService service =
            new ArticleService(repository, mock(MongoOperations.class));

    @Test
    void combinesOnlyPublicSourcesAndRequestsDeterministicGlobalPage() {
        var now = LocalDateTime.of(2026, 9, 27, 12, 0);
        var rows =
                List.of(
                        Article.builder().id("c").source("other").publishedAt(now).build(),
                        Article.builder().id("b").source("rookie").publishedAt(now).build(),
                        Article.builder()
                                .id("a")
                                .source("jumpball")
                                .publishedAt(now.minusDays(1))
                                .build());
        when(repository.findBySourceIn(anyCollection(), any(Pageable.class)))
                .thenAnswer(
                        i -> {
                            Collection<String> sources = i.getArgument(0);
                            Pageable p = i.getArgument(1);
                            assertThat(p.getSort())
                                    .isEqualTo(Sort.by(Sort.Direction.DESC, "publishedAt", "id"));
                            var filtered =
                                    rows.stream()
                                            .filter(a -> sources.contains(a.getSource()))
                                            .toList();
                            return new PageImpl<>(
                                    filtered.stream()
                                            .skip(p.getOffset())
                                            .limit(p.getPageSize())
                                            .toList(),
                                    p,
                                    filtered.size());
                        });
        var first = service.getPage("all", 0, 2);
        assertThat(first.getArticles()).extracting("id").containsExactly("c", "b");
        assertThat(first.getTotal()).isEqualTo(3);
        assertThat(first.isHasNext()).isTrue();
        assertThat(service.getPage("all", 1, 2).getArticles())
                .extracting("id")
                .containsExactly("a");
        assertThat(service.getPage("all", 2, 2).getArticles()).isEmpty();
        assertThat(service.getPage("rookie", 0, 2).getArticles())
                .extracting("id")
                .containsExactly("b");
        verify(repository, times(3))
                .findBySourceIn(eq(List.of("jumpball", "rookie", "other")), any(Pageable.class));
    }

    @Test
    void existingSourceRouteAddsIdTieBreakWithoutChangingRepositoryDateOrder() {
        when(repository.findBySourceOrderByPublishedAtDesc(eq("other"), any(Pageable.class)))
                .thenAnswer(
                        i -> {
                            Pageable p = i.getArgument(1);
                            assertThat(p.getSort()).isEqualTo(Sort.by(Sort.Direction.DESC, "id"));
                            return new PageImpl<Article>(List.of(), p, 0);
                        });
        assertThat(service.getBySource("other", 0, 8).getTotal()).isZero();
    }
}
