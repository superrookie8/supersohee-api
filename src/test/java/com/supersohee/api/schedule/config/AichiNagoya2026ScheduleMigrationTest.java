package com.supersohee.api.schedule.config;

import com.mongodb.client.result.UpdateResult;
import com.supersohee.api.schedule.domain.Schedule;
import org.bson.BsonString;
import org.bson.Document;
import org.junit.jupiter.api.Test;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SuppressWarnings("unchecked")
class AichiNagoya2026ScheduleMigrationTest {
    private final MongoOperations mongo = mock(MongoOperations.class);
    private final AichiNagoya2026ScheduleMigration migration = new AichiNagoya2026ScheduleMigration(mongo);

    @Test
    void firstRunInsertsThreeDeterministicSchedulesBeforeWritingMarker() throws Exception {
        when(mongo.exists(any(Query.class), eq("data_migrations"))).thenReturn(false);
        when(mongo.find(any(Query.class), eq(Schedule.class))).thenReturn(List.of());
        when(mongo.upsert(any(Query.class), any(Update.class), eq(Schedule.class)))
                .thenReturn(UpdateResult.acknowledged(0, 0L, new BsonString("inserted")));

        migration.run(new DefaultApplicationArguments());

        var ordered = inOrder(mongo);
        ordered.verify(mongo, times(3)).upsert(any(Query.class), any(Update.class), eq(Schedule.class));
        ordered.verify(mongo).upsert(
                org.mockito.ArgumentMatchers.argThat(query ->
                        "schedules-v1-aichi-nagoya-2026-group-stage".equals(
                                query.getQueryObject().getString("_id"))),
                any(Update.class), eq("data_migrations"));

        var queryCaptor = org.mockito.ArgumentCaptor.forClass(Query.class);
        verify(mongo, times(3)).upsert(queryCaptor.capture(), any(Update.class), eq(Schedule.class));
        assertThat(queryCaptor.getAllValues())
                .extracting(query -> query.getQueryObject().getString("_id"))
                .containsExactly(
                        "aichi-nagoya-2026-women-group-malaysia",
                        "aichi-nagoya-2026-women-group-mongolia",
                        "aichi-nagoya-2026-women-group-chinese-taipei");
    }

    @Test
    void oneLegacyMatchIsCanonicalizedInsteadOfDuplicated() throws Exception {
        Schedule legacy = Schedule.builder().id("existing-random-id")
                .startDateTime(LocalDateTime.of(2026, 9, 18, 10, 0))
                .title("Malaysia").build();
        when(mongo.exists(any(Query.class), eq("data_migrations"))).thenReturn(false);
        when(mongo.find(any(Query.class), eq(Schedule.class))).thenReturn(
                List.of(), List.of(legacy),
                List.of(), List.of(),
                List.of(), List.of());
        when(mongo.updateFirst(any(Query.class), any(Update.class), eq(Schedule.class)))
                .thenReturn(UpdateResult.acknowledged(1, 1L, null));
        when(mongo.upsert(any(Query.class), any(Update.class), eq(Schedule.class)))
                .thenReturn(UpdateResult.acknowledged(0, 0L, new BsonString("inserted")));

        migration.run(new DefaultApplicationArguments());

        verify(mongo).updateFirst(
                org.mockito.ArgumentMatchers.argThat(query ->
                        "existing-random-id".equals(query.getQueryObject().getString("_id"))),
                org.mockito.ArgumentMatchers.argThat(update -> {
                    Document set = update.getUpdateObject().get("$set", Document.class);
                    return "말레이시아".equals(set.getString("opponent"))
                            && "2026-aichi-nagoya-asian-games".equals(set.getString("competitionKey"));
                }),
                eq(Schedule.class));
        verify(mongo, times(2)).upsert(any(Query.class), any(Update.class), eq(Schedule.class));
    }

    @Test
    void ambiguousLegacyMatchesFailWithoutWritingSchedulesOrMarker() {
        Schedule first = Schedule.builder().id("first").build();
        Schedule second = Schedule.builder().id("second").build();
        when(mongo.exists(any(Query.class), eq("data_migrations"))).thenReturn(false);
        when(mongo.find(any(Query.class), eq(Schedule.class)))
                .thenReturn(List.of(), List.of(first, second));

        assertThatThrownBy(() -> migration.run(new DefaultApplicationArguments()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Aichi-Nagoya schedule migration could not be completed.")
                .hasNoCause();
        verify(mongo, never()).updateFirst(any(Query.class), any(Update.class), eq(Schedule.class));
        verify(mongo, never()).upsert(any(Query.class), any(Update.class), eq(Schedule.class));
        verify(mongo, never()).upsert(any(Query.class), any(Update.class), eq("data_migrations"));
    }

    @Test
    void existingMarkerSkipsAllScheduleReadsAndWrites() throws Exception {
        when(mongo.exists(any(Query.class), eq("data_migrations"))).thenReturn(true);

        migration.run(new DefaultApplicationArguments());

        verify(mongo, never()).find(any(Query.class), eq(Schedule.class));
        verify(mongo, never()).updateFirst(any(Query.class), any(Update.class), eq(Schedule.class));
        verify(mongo, never()).upsert(any(Query.class), any(Update.class), eq(Schedule.class));
    }

    @Test
    void canonicalFieldsUseNationalTournamentAndConfirmedNeutralVenue() {
        var games = AichiNagoya2026ScheduleMigration.games();
        assertThat(games).extracting(AichiNagoya2026ScheduleMigration.GameSeed::startDateTime)
                .containsExactly(
                        LocalDateTime.of(2026, 9, 18, 10, 0),
                        LocalDateTime.of(2026, 9, 21, 13, 0),
                        LocalDateTime.of(2026, 9, 22, 16, 0));
        assertThat(games).extracting(AichiNagoya2026ScheduleMigration.GameSeed::opponent)
                .containsExactly("말레이시아", "몽골", "대만");

        LocalDateTime now = LocalDateTime.of(2026, 9, 10, 12, 0);
        Document update = AichiNagoya2026ScheduleMigration.canonicalUpdate(games.get(0), now)
                .getUpdateObject().get("$set", Document.class);
        assertThat(update)
                .containsEntry("competitionKey", "2026-aichi-nagoya-asian-games")
                .containsEntry("competitionName", "아이치·나고야 아시안게임 여자농구")
                .containsEntry("competitionKind", "tournament")
                .containsEntry("editionLabel", "2026")
                .containsEntry("teamType", "national")
                .containsEntry("ourTeamName", "대한민국")
                .containsEntry("venueType", "neutral")
                .containsEntry("venueName", "아이치 국제 아레나(IG 아레나)")
                .containsEntry("location", "아이치 국제 아레나(IG 아레나)")
                .containsEntry("stage", "조별리그")
                .containsEntry("isHome", false)
                .containsEntry("isActive", true)
                .containsEntry("updatedAt", now);
    }
}
