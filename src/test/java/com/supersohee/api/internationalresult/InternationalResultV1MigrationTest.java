package com.supersohee.api.internationalresult;

import com.supersohee.api.internationalresult.config.InternationalResultV1Migration;
import com.supersohee.api.internationalresult.domain.InternationalResult;
import com.supersohee.api.internationalresult.domain.InternationalResultSource;
import com.supersohee.api.internationalresult.domain.InternationalResultStatus;
import org.bson.Document;
import org.junit.jupiter.api.Test;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.index.IndexDefinition;
import org.springframework.data.mongodb.core.index.IndexOperations;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class InternationalResultV1MigrationTest {
    private final MongoOperations mongo = mock(MongoOperations.class);
    private final IndexOperations indexes = mock(IndexOperations.class);
    private final InternationalResultV1Migration migration = new InternationalResultV1Migration(mongo);

    @Test
    void insertsEveryStableKeyBeforeWritingCompletionMarker() throws Exception {
        when(mongo.indexOps(InternationalResult.class)).thenReturn(indexes);
        when(mongo.exists(any(Query.class), eq("data_migrations"))).thenReturn(false);

        migration.run(new DefaultApplicationArguments());

        verify(indexes).createIndex(any(IndexDefinition.class));
        verify(mongo, times(10)).upsert(any(Query.class), any(Update.class), eq(InternationalResult.class));
        verify(mongo).upsert(any(Query.class), any(Update.class), eq("data_migrations"));
        var ordered = inOrder(mongo);
        ordered.verify(mongo, times(10)).upsert(any(Query.class), any(Update.class), eq(InternationalResult.class));
        ordered.verify(mongo).upsert(any(Query.class), any(Update.class), eq("data_migrations"));
    }

    @Test
    void markerSkipsSeedsButStillEnsuresRequiredUniqueIndex() throws Exception {
        when(mongo.indexOps(InternationalResult.class)).thenReturn(indexes);
        when(mongo.exists(any(Query.class), eq("data_migrations"))).thenReturn(true);

        migration.run(new DefaultApplicationArguments());

        verify(indexes).createIndex(any(IndexDefinition.class));
        verify(mongo, never()).upsert(any(Query.class), any(Update.class), eq(InternationalResult.class));
    }

    @Test
    void failedSeedDoesNotWriteMarkerAndFailureMessageDoesNotLeakData() {
        when(mongo.indexOps(InternationalResult.class)).thenReturn(indexes);
        when(mongo.exists(any(Query.class), eq("data_migrations"))).thenReturn(false);
        AtomicBoolean first = new AtomicBoolean(true);
        when(mongo.upsert(any(Query.class), any(Update.class), eq(InternationalResult.class)))
                .thenAnswer(invocation -> {
                    if (first.getAndSet(false)) {
                        throw new IllegalStateException("sensitive stored value");
                    }
                    return null;
                });

        org.assertj.core.api.Assertions.assertThatThrownBy(
                        () -> migration.run(new DefaultApplicationArguments()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("International result migration v1 could not be completed.")
                .hasNoCause();
        verify(mongo, never()).upsert(any(Query.class), any(Update.class), eq("data_migrations"));
    }

    @Test
    void newDatabaseSeedContainsTheFinal2026WorldCupResult() throws Exception {
        when(mongo.indexOps(InternationalResult.class)).thenReturn(indexes);
        when(mongo.exists(any(Query.class), eq("data_migrations"))).thenReturn(false);

        migration.run(new DefaultApplicationArguments());

        var queryCaptor = org.mockito.ArgumentCaptor.forClass(Query.class);
        var updateCaptor = org.mockito.ArgumentCaptor.forClass(Update.class);
        verify(mongo, times(10)).upsert(
                queryCaptor.capture(), updateCaptor.capture(), eq(InternationalResult.class));
        int worldCupIndex = java.util.stream.IntStream.range(0, queryCaptor.getAllValues().size())
                .filter(index -> "2026-fiba-womens-world-cup".equals(
                        queryCaptor.getAllValues().get(index).getQueryObject().getString("competitionKey")))
                .findFirst()
                .orElseThrow();
        Document inserted = updateCaptor.getAllValues().get(worldCupIndex)
                .getUpdateObject().get("$setOnInsert", Document.class);

        assertThat(inserted)
                .containsEntry("status", InternationalResultStatus.FINAL)
                .containsEntry("teamResult", "B조 3위 · 8강 진출 결정전")
                .containsEntry("teamRecord", "1승 3패")
                .containsEntry("gamesPlayed", 4)
                .containsEntry("minutesPerGame", "17:33")
                .containsEntry("pointsPerGame", 8.0)
                .containsEntry("reboundsPerGame", 1.0)
                .containsEntry("assistsPerGame", 1.0)
                .containsEntry("stealsPerGame", 1.8)
                .containsEntry("fieldGoalPercent", 40.0)
                .containsEntry("threePointPercent", 38.5)
                .containsEntry("freeThrowPercent", 100.0)
                .containsEntry("highlight", "헝가리전에서 12점, 3어시스트와 2스틸을 기록하며 이번 대회 개인 최다 득점을 올렸습니다.")
                .containsEntry("statsUpdatedThrough", null);
        @SuppressWarnings("unchecked")
        var sources = (java.util.List<InternationalResultSource>) inserted.get("sources");
        assertThat(sources).extracting(InternationalResultSource::getUrl)
                .containsExactly(
                        "https://www.fiba.basketball/en/events/fiba-womens-basketball-world-cup-2026/teams/korea/219255-sohee-lee",
                        "https://www.fiba.basketball/en/events/fiba-womens-basketball-world-cup-2026/games/128126-HUN-KOR");
    }
}
