package com.supersohee.api.article.service;

import com.supersohee.api.article.domain.Article;
import com.supersohee.api.article.dto.AdminArticleImportItem;
import com.supersohee.api.article.dto.AdminArticleImportRequest;
import com.supersohee.api.article.dto.AdminArticleImportResponse;
import com.supersohee.api.article.dto.AdminManualArticleRequest;
import com.supersohee.api.article.repository.ArticleRepository;
import com.mongodb.client.result.UpdateResult;
import org.bson.BsonString;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ArticleServiceAdminTest {

    @Test
    void atomicUpsertClassifiesCreatedAndExistingByUpsertedId() {
        ArticleRepository repository = mock(ArticleRepository.class);
        MongoOperations mongoOperations = mock(MongoOperations.class);
        ArticleService service = new ArticleService(repository, mongoOperations);
        when(mongoOperations.upsert(any(Query.class), any(Update.class), eq(Article.class)))
                .thenReturn(UpdateResult.acknowledged(0, 0L, new BsonString("created-id")))
                .thenReturn(UpdateResult.acknowledged(1, 0L, null));
        AdminArticleImportRequest request = new AdminArticleImportRequest(List.of(
                new AdminArticleImportItem(
                        "jumpball", "기사", "https://jumpball.test/1", "요약", null,
                        LocalDateTime.of(2026, 1, 1, 12, 0))));

        AdminArticleImportResponse first = service.importArticles(request);
        AdminArticleImportResponse retry = service.importArticles(request);

        assertThat(first.created()).isEqualTo(1);
        assertThat(first.existing()).isZero();
        assertThat(retry.created()).isZero();
        assertThat(retry.existing()).isEqualTo(1);
        verify(mongoOperations, times(2)).upsert(any(Query.class), any(Update.class), eq(Article.class));
    }

    @Test
    void duplicateUpsertRaceCountsOnlyTheSamePersistedIdentityAsExisting() {
        ArticleRepository repository = mock(ArticleRepository.class);
        MongoOperations mongoOperations = mock(MongoOperations.class);
        ArticleService service = new ArticleService(repository, mongoOperations);
        when(mongoOperations.upsert(any(Query.class), any(Update.class), eq(Article.class)))
                .thenThrow(new DuplicateKeyException("race fixture"));
        when(mongoOperations.exists(any(Query.class), eq(Article.class))).thenReturn(true);

        AdminArticleImportResponse response = service.importArticles(singleArticleRequest());

        assertThat(response.processed()).isEqualTo(1);
        assertThat(response.created()).isZero();
        assertThat(response.existing()).isEqualTo(1);
    }

    @Test
    void unrelatedDuplicateAndMidBatchStorageFailureAreNotHiddenAsSuccess() {
        ArticleRepository repository = mock(ArticleRepository.class);
        MongoOperations mongoOperations = mock(MongoOperations.class);
        ArticleService service = new ArticleService(repository, mongoOperations);
        when(mongoOperations.upsert(any(Query.class), any(Update.class), eq(Article.class)))
                .thenThrow(new DuplicateKeyException("unrelated fixture"));
        when(mongoOperations.exists(any(Query.class), eq(Article.class))).thenReturn(false);

        assertThatExceptionOfType(DuplicateKeyException.class)
                .isThrownBy(() -> service.importArticles(singleArticleRequest()));

        MongoOperations partialMongo = mock(MongoOperations.class);
        ArticleService partialService = new ArticleService(repository, partialMongo);
        when(partialMongo.upsert(any(Query.class), any(Update.class), eq(Article.class)))
                .thenReturn(UpdateResult.acknowledged(0, 0L, new BsonString("first-id")))
                .thenThrow(new DataAccessResourceFailureException("fixture unavailable"));
        AdminArticleImportRequest twoArticles = new AdminArticleImportRequest(List.of(
                singleArticleRequest().articles().get(0),
                new AdminArticleImportItem(
                        "rookie", "두 번째 기사", "https://rookie.test/2", null, null,
                        LocalDateTime.of(2026, 1, 2, 12, 0))));

        assertThatExceptionOfType(DataAccessResourceFailureException.class)
                .isThrownBy(() -> partialService.importArticles(twoArticles));
        verify(partialMongo, times(2)).upsert(any(Query.class), any(Update.class), eq(Article.class));
    }

    @Test
    void manualArticleMapsContentToSummaryAndManualSource() {
        ArticleRepository repository = mock(ArticleRepository.class);
        MongoOperations mongoOperations = mock(MongoOperations.class);
        ArticleService service = new ArticleService(repository, mongoOperations);
        when(repository.save(any(Article.class))).thenAnswer(invocation -> invocation.getArgument(0, Article.class));

        Article result = service.createManualArticle(new AdminManualArticleRequest(" 제목 ", " 내용 "));

        assertThat(result.getSource()).isEqualTo("manual");
        assertThat(result.getTitle()).isEqualTo("제목");
        assertThat(result.getSummary()).isEqualTo("내용");
        assertThat(result.getUrl()).isNull();
    }

    @Test
    void publicSourcePageUsesThePublishedAtDescendingRepositoryContract() {
        ArticleRepository repository = mock(ArticleRepository.class);
        MongoOperations mongoOperations = mock(MongoOperations.class);
        ArticleService service = new ArticleService(repository, mongoOperations);
        Article latest = Article.builder().id("latest").source("jumpball").build();
        when(repository.findBySourceOrderByPublishedAtDesc("jumpball", PageRequest.of(0, 1)))
                .thenReturn(new PageImpl<>(List.of(latest), PageRequest.of(0, 1), 1));

        var response = service.getBySource("jumpball", 0, 1);

        assertThat(response.getArticles()).extracting(item -> item.id()).containsExactly("latest");
        assertThat(response.getTotal()).isEqualTo(1);
        verify(repository).findBySourceOrderByPublishedAtDesc("jumpball", PageRequest.of(0, 1));
    }

    private AdminArticleImportRequest singleArticleRequest() {
        return new AdminArticleImportRequest(List.of(new AdminArticleImportItem(
                "jumpball", "기사", "https://jumpball.test/1", "요약", null,
                LocalDateTime.of(2026, 1, 1, 12, 0))));
    }
}
