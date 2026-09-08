package com.supersohee.api.internationalresult.config;

import com.supersohee.api.internationalresult.domain.InternationalResult;
import com.supersohee.api.internationalresult.domain.InternationalResultSource;
import com.supersohee.api.internationalresult.domain.InternationalResultSourceType;
import org.bson.Document;
import org.junit.jupiter.api.Test;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class InternationalResultV2MigrationTest {
    private final MongoOperations mongo = mock(MongoOperations.class);
    private final InternationalResultV2Migration migration = new InternationalResultV2Migration(mongo);

    @Test
    void firstRunInsertsJonesCupBeforeWritingCompletionMarker() throws Exception {
        when(mongo.exists(any(Query.class), eq("data_migrations"))).thenReturn(false);

        migration.run(new DefaultApplicationArguments());

        var ordered = inOrder(mongo);
        ordered.verify(mongo).upsert(
                org.mockito.ArgumentMatchers.argThat(query ->
                        "2023-william-jones-cup".equals(query.getQueryObject().getString("competitionKey"))),
                any(Update.class), eq(InternationalResult.class));
        ordered.verify(mongo).upsert(
                org.mockito.ArgumentMatchers.argThat(query ->
                        "international-results-v2".equals(query.getQueryObject().getString("_id"))),
                any(Update.class), eq("data_migrations"));
    }

    @Test
    void existingMarkerSkipsTheCompetitionUpsert() throws Exception {
        when(mongo.exists(any(Query.class), eq("data_migrations"))).thenReturn(true);

        migration.run(new DefaultApplicationArguments());

        verify(mongo, never()).upsert(any(Query.class), any(Update.class), eq(InternationalResult.class));
        verify(mongo, never()).upsert(any(Query.class), any(Update.class), eq("data_migrations"));
    }

    @Test
    void failedCompetitionUpsertDoesNotWriteMarkerAndHidesStoredDiagnostics() {
        when(mongo.exists(any(Query.class), eq("data_migrations"))).thenReturn(false);
        when(mongo.upsert(any(Query.class), any(Update.class), eq(InternationalResult.class)))
                .thenThrow(new IllegalStateException("sensitive stored value"));

        assertThatThrownBy(() -> migration.run(new DefaultApplicationArguments()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("International result migration v2 could not be completed.")
                .hasNoCause();
        verify(mongo, never()).upsert(any(Query.class), any(Update.class), eq("data_migrations"));
    }

    @Test
    void competitionUpdateUsesOnlySetOnInsertAndCarriesVerifiedSources() {
        Update update = InternationalResultV2Migration.insertOnly(LocalDateTime.of(2026, 9, 9, 12, 0));
        Document updateDocument = update.getUpdateObject();

        assertThat(updateDocument.keySet()).containsExactly("$setOnInsert");
        Document inserted = updateDocument.get("$setOnInsert", Document.class);
        assertThat(inserted.getString("competitionKey")).isEqualTo("2023-william-jones-cup");
        assertThat(inserted.getDouble("pointsPerGame")).isEqualTo(18.2);
        assertThat(inserted.getInteger("displayOrder")).isEqualTo(75);
        assertThat(inserted).containsKeys("reboundsPerGame", "assistsPerGame", "stealsPerGame");
        @SuppressWarnings("unchecked")
        var sources = (java.util.List<InternationalResultSource>) inserted.get("sources");
        assertThat(sources).extracting(InternationalResultSource::getType)
                .containsExactly(InternationalResultSourceType.OFFICIAL, InternationalResultSourceType.SECONDARY);
        assertThat(sources).extracting(InternationalResultSource::getUrl)
                .containsExactly(
                        "https://www.wkbl.or.kr/news/teamstory_view.asp?num=36969&page=490",
                        "https://www.yna.co.kr/view/AKR20230809163500007");
        assertThat(updateDocument).doesNotContainKeys("$set", "$unset");
    }
}
