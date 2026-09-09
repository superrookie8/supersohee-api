package com.supersohee.api.schedule.config;

import com.mongodb.client.result.UpdateResult;
import com.supersohee.api.schedule.domain.Schedule;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Profile("!test")
@ConditionalOnProperty(
        prefix = "app.schedule-migration",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true)
public class AichiNagoya2026ScheduleMigration implements ApplicationRunner {
    static final String MIGRATION_ID = "schedules-v1-aichi-nagoya-2026-group-stage";
    static final String MIGRATION_COLLECTION = "data_migrations";
    static final String COMPETITION_KEY = "2026-aichi-nagoya-asian-games";
    static final String VENUE_NAME = "아이치 국제 아레나(IG 아레나)";

    private final MongoOperations mongoOperations;

    @Override
    public void run(ApplicationArguments args) {
        try {
            Query marker = Query.query(Criteria.where("_id").is(MIGRATION_ID));
            if (mongoOperations.exists(marker, MIGRATION_COLLECTION)) {
                return;
            }

            LocalDateTime now = LocalDateTime.now();
            for (GameSeed game : games()) {
                applyGame(game, now);
            }
            mongoOperations.upsert(marker,
                    new Update().setOnInsert("appliedAt", LocalDateTime.now()),
                    MIGRATION_COLLECTION);
        } catch (RuntimeException failure) {
            // Stored values can appear in Mongo diagnostics. Keep startup output generic.
            throw new IllegalStateException("Aichi-Nagoya schedule migration could not be completed.");
        }
    }

    private void applyGame(GameSeed game, LocalDateTime now) {
        List<Schedule> canonical = mongoOperations.find(canonicalQuery(game), Schedule.class);
        if (canonical.size() > 1) {
            throw new IllegalStateException("Ambiguous canonical schedule.");
        }
        List<Schedule> legacy = mongoOperations.find(legacyQuery(game), Schedule.class);
        if (legacy.size() > 1) {
            throw new IllegalStateException("Ambiguous legacy schedule.");
        }
        if (canonical.size() + legacy.size() > 1) {
            throw new IllegalStateException("Duplicate schedule identities exist.");
        }
        if (canonical.size() == 1) {
            updateExisting(canonical.get(0), game, now);
            return;
        }
        if (legacy.size() == 1) {
            updateExisting(legacy.get(0), game, now);
            return;
        }

        UpdateResult inserted = mongoOperations.upsert(
                Query.query(Criteria.where("_id").is(game.id())),
                canonicalUpdate(game, now).setOnInsert("createdAt", now),
                Schedule.class);
        if (!inserted.wasAcknowledged()
                || (inserted.getMatchedCount() != 1 && inserted.getUpsertedId() == null)) {
            throw new IllegalStateException("Schedule was not inserted.");
        }
    }

    private void updateExisting(Schedule schedule, GameSeed game, LocalDateTime now) {
        if (schedule.getId() == null || schedule.getId().isBlank()) {
            throw new IllegalStateException("Schedule identity is missing.");
        }
        UpdateResult updated = mongoOperations.updateFirst(
                Query.query(Criteria.where("_id").is(schedule.getId())),
                canonicalUpdate(game, now),
                Schedule.class);
        if (!updated.wasAcknowledged() || updated.getMatchedCount() != 1) {
            throw new IllegalStateException("Schedule was not updated.");
        }
    }

    static Query canonicalQuery(GameSeed game) {
        return Query.query(new Criteria().andOperator(
                Criteria.where("competitionKey").is(COMPETITION_KEY),
                Criteria.where("startDateTime").is(game.startDateTime())));
    }

    static Query legacyQuery(GameSeed game) {
        Criteria missingCompetition = new Criteria().orOperator(
                Criteria.where("competitionKey").exists(false),
                Criteria.where("competitionKey").is(null),
                Criteria.where("competitionKey").is(""));
        Criteria opponent = new Criteria().orOperator(
                Criteria.where("opponent").in(game.aliases()),
                Criteria.where("title").in(game.aliases()));
        Criteria compatibleTeamType = new Criteria().orOperator(
                Criteria.where("teamType").exists(false),
                Criteria.where("teamType").is(null),
                Criteria.where("teamType").is("national"));
        Criteria compatibleOurTeam = new Criteria().orOperator(
                Criteria.where("ourTeamName").exists(false),
                Criteria.where("ourTeamName").is(null),
                Criteria.where("ourTeamName").is("대한민국"));
        return Query.query(new Criteria().andOperator(
                Criteria.where("startDateTime").is(game.startDateTime()),
                missingCompetition, opponent, compatibleTeamType, compatibleOurTeam));
    }

    static Update canonicalUpdate(GameSeed game, LocalDateTime now) {
        return new Update()
                .set("title", game.opponent())
                .set("opponent", game.opponent())
                .set("startDateTime", game.startDateTime())
                // AdminScheduleRequest uses the same two-hour game duration.
                .set("endDateTime", game.startDateTime().plusHours(2))
                .set("location", VENUE_NAME)
                .set("type", "game")
                .set("color", "#3B82F6")
                .set("competitionKey", COMPETITION_KEY)
                .set("competitionName", "아이치·나고야 아시안게임 여자농구")
                .set("competitionKind", "tournament")
                .set("editionLabel", "2026")
                .set("teamType", "national")
                .set("ourTeamName", "대한민국")
                .set("venueType", "neutral")
                .set("venueName", VENUE_NAME)
                .set("stage", "조별리그")
                .set("isHome", false)
                .set("specialGame", false)
                .set("isActive", true)
                .set("updatedAt", now);
    }

    static List<GameSeed> games() {
        return List.of(
                new GameSeed("aichi-nagoya-2026-women-group-malaysia",
                        LocalDateTime.of(2026, 9, 18, 10, 0), "말레이시아", List.of("말레이시아", "Malaysia")),
                new GameSeed("aichi-nagoya-2026-women-group-mongolia",
                        LocalDateTime.of(2026, 9, 21, 13, 0), "몽골", List.of("몽골", "Mongolia")),
                new GameSeed("aichi-nagoya-2026-women-group-chinese-taipei",
                        LocalDateTime.of(2026, 9, 22, 16, 0), "대만",
                        List.of("대만", "차이니스 타이베이", "Chinese Taipei")));
    }

    record GameSeed(String id, LocalDateTime startDateTime, String opponent, List<String> aliases) {
    }
}
