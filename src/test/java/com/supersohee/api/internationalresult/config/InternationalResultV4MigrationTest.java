package com.supersohee.api.internationalresult.config;

import com.mongodb.client.result.UpdateResult;
import com.supersohee.api.internationalresult.domain.InternationalResult;
import com.supersohee.api.internationalresult.domain.InternationalResultSource;
import com.supersohee.api.internationalresult.domain.InternationalResultSourceType;
import org.bson.Document;
import org.junit.jupiter.api.Test;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class InternationalResultV4MigrationTest {
    private final MongoOperations mongo = mock(MongoOperations.class);
    private final InternationalResultV4Migration migration = new InternationalResultV4Migration(mongo);

    @Test
    void oneMatchingResultIsUpdatedBeforeCompletionMarkerIsWritten() throws Exception {
        when(mongo.exists(any(Query.class), eq("data_migrations"))).thenReturn(false);
        when(mongo.updateFirst(any(Query.class), any(Update.class), eq(InternationalResult.class)))
                .thenReturn(UpdateResult.acknowledged(1, 1L, null));

        migration.run(new DefaultApplicationArguments());

        var ordered = inOrder(mongo);
        ordered.verify(mongo).updateFirst(
                org.mockito.ArgumentMatchers.argThat(query ->
                        "2026-fiba-womens-world-cup".equals(
                                query.getQueryObject().getString("competitionKey"))),
                any(Update.class), eq(InternationalResult.class));
        ordered.verify(mongo).upsert(
                org.mockito.ArgumentMatchers.argThat(query ->
                        "international-results-v4-world-cup-2026-highlight".equals(
                                query.getQueryObject().getString("_id"))),
                any(Update.class), eq("data_migrations"));
    }

    @Test
    void noMatchingResultFailsStartupAndDoesNotWriteMarker() {
        when(mongo.exists(any(Query.class), eq("data_migrations"))).thenReturn(false);
        when(mongo.updateFirst(any(Query.class), any(Update.class), eq(InternationalResult.class)))
                .thenReturn(UpdateResult.acknowledged(0, 0L, null));

        assertThatThrownBy(() -> migration.run(new DefaultApplicationArguments()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("International result migration v4 could not be completed.")
                .hasNoCause();
        verify(mongo, never()).upsert(any(Query.class), any(Update.class), eq("data_migrations"));
    }

    @Test
    void existingMarkerSkipsTheResultUpdate() throws Exception {
        when(mongo.exists(any(Query.class), eq("data_migrations"))).thenReturn(true);

        migration.run(new DefaultApplicationArguments());

        verify(mongo, never()).updateFirst(any(Query.class), any(Update.class), eq(InternationalResult.class));
        verify(mongo, never()).upsert(any(Query.class), any(Update.class), eq("data_migrations"));
    }

    @Test
    void exactKeyUpdateSetsOnlyHighlightAndOfficialSources() {
        assertThat(InternationalResultV4Migration.competitionQuery().getQueryObject())
                .containsExactly(new java.util.AbstractMap.SimpleEntry<>(
                        "competitionKey", "2026-fiba-womens-world-cup"));

        Document updateDocument = InternationalResultV4Migration.highlightUpdate().getUpdateObject();
        assertThat(updateDocument.keySet()).containsExactly("$set");
        Document set = updateDocument.get("$set", Document.class);
        assertThat(set.keySet()).containsExactly("highlight", "sources");
        assertThat(set.getString("highlight"))
                .isEqualTo("헝가리전에서 12점, 3어시스트와 2스틸을 기록하며 이번 대회 개인 최다 득점을 올렸습니다.");

        @SuppressWarnings("unchecked")
        var sources = (java.util.List<InternationalResultSource>) set.get("sources");
        assertThat(sources).extracting(InternationalResultSource::getType)
                .containsOnly(InternationalResultSourceType.OFFICIAL);
        assertThat(sources).extracting(InternationalResultSource::getLabel)
                .containsExactly(
                        "FIBA 2026 월드컵 이소희 선수 기록",
                        "FIBA 2026 월드컵 헝가리-대한민국 공식 경기");
        assertThat(sources).extracting(InternationalResultSource::getUrl)
                .containsExactly(
                        "https://www.fiba.basketball/en/events/fiba-womens-basketball-world-cup-2026/teams/korea/219255-sohee-lee",
                        "https://www.fiba.basketball/en/events/fiba-womens-basketball-world-cup-2026/games/128126-HUN-KOR");
    }
}
