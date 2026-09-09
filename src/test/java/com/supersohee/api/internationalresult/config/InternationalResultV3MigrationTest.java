package com.supersohee.api.internationalresult.config;

import com.mongodb.client.result.UpdateResult;
import com.supersohee.api.internationalresult.domain.InternationalResult;
import com.supersohee.api.internationalresult.domain.InternationalResultSource;
import com.supersohee.api.internationalresult.domain.InternationalResultSourceType;
import com.supersohee.api.internationalresult.domain.InternationalResultStatus;
import com.supersohee.api.internationalresult.domain.ParticipationStatus;
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

class InternationalResultV3MigrationTest {
    private final MongoOperations mongo = mock(MongoOperations.class);
    private final InternationalResultV3Migration migration = new InternationalResultV3Migration(mongo);

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
                        "international-results-v3-final-world-cup-2026".equals(
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
                .hasMessage("International result migration v3 could not be completed.")
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
    void exactKeyUpdateSetsOnlyFinalFieldsAndOfficialSources() {
        LocalDateTime now = LocalDateTime.of(2026, 9, 9, 18, 30);

        assertThat(InternationalResultV3Migration.competitionQuery().getQueryObject())
                .containsExactly(new java.util.AbstractMap.SimpleEntry<>(
                        "competitionKey", "2026-fiba-womens-world-cup"));

        Document updateDocument = InternationalResultV3Migration.finalResultUpdate(now).getUpdateObject();
        assertThat(updateDocument.keySet()).containsExactly("$set");
        Document set = updateDocument.get("$set", Document.class);
        assertThat(set)
                .containsEntry("status", InternationalResultStatus.FINAL)
                .containsEntry("participationStatus", ParticipationStatus.CONFIRMED)
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
                .containsEntry("statsUpdatedThrough", null)
                .containsEntry("updatedAt", now);
        assertThat(set).doesNotContainKeys(
                "competitionKey", "competitionName", "editionLabel", "startDate", "endDate",
                "location", "teamName", "published", "displayOrder", "createdAt");

        @SuppressWarnings("unchecked")
        var sources = (java.util.List<InternationalResultSource>) set.get("sources");
        assertThat(sources).extracting(InternationalResultSource::getType)
                .containsOnly(InternationalResultSourceType.OFFICIAL);
        assertThat(sources).extracting(InternationalResultSource::getUrl)
                .containsExactly(
                        "https://www.fiba.basketball/en/events/fiba-womens-basketball-world-cup-2026/teams/korea/219255-sohee-lee",
                        "https://www.fiba.basketball/en/events/fiba-womens-basketball-world-cup-2026/games/128144-GER-KOR");
    }
}
