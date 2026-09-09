package com.supersohee.api.internationalresult.config;

import com.mongodb.client.result.UpdateResult;
import com.supersohee.api.internationalresult.domain.InternationalResult;
import com.supersohee.api.internationalresult.domain.InternationalResultSource;
import com.supersohee.api.internationalresult.domain.InternationalResultSourceType;
import com.supersohee.api.internationalresult.domain.InternationalResultStatus;
import com.supersohee.api.internationalresult.domain.ParticipationStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
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
@Order(300)
@ConditionalOnProperty(
        prefix = "app.international-results-migration",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true)
public class InternationalResultV3Migration implements ApplicationRunner {
    static final String MIGRATION_ID = "international-results-v3-final-world-cup-2026";
    static final String MIGRATION_COLLECTION = "data_migrations";
    static final String COMPETITION_KEY = "2026-fiba-womens-world-cup";

    private final MongoOperations mongoOperations;

    @Override
    public void run(ApplicationArguments args) {
        try {
            Query marker = Query.query(Criteria.where("_id").is(MIGRATION_ID));
            if (mongoOperations.exists(marker, MIGRATION_COLLECTION)) {
                return;
            }

            UpdateResult result = mongoOperations.updateFirst(
                    competitionQuery(), finalResultUpdate(LocalDateTime.now()), InternationalResult.class);
            if (result.getMatchedCount() != 1) {
                throw new IllegalStateException("Expected one world cup result.");
            }

            mongoOperations.upsert(marker,
                    new Update().setOnInsert("appliedAt", LocalDateTime.now()),
                    MIGRATION_COLLECTION);
        } catch (RuntimeException failure) {
            // Mongo diagnostics can contain stored values. Do not expose the cause at startup.
            throw new IllegalStateException("International result migration v3 could not be completed.");
        }
    }

    static Query competitionQuery() {
        return Query.query(Criteria.where("competitionKey").is(COMPETITION_KEY));
    }

    static Update finalResultUpdate(LocalDateTime now) {
        List<InternationalResultSource> sources = List.of(
                InternationalResultSource.builder()
                        .label("FIBA 2026 월드컵 이소희 선수 기록")
                        .url("https://www.fiba.basketball/en/events/fiba-womens-basketball-world-cup-2026/teams/korea/219255-sohee-lee")
                        .type(InternationalResultSourceType.OFFICIAL)
                        .build(),
                InternationalResultSource.builder()
                        .label("FIBA 2026 월드컵 독일-대한민국 공식 경기")
                        .url("https://www.fiba.basketball/en/events/fiba-womens-basketball-world-cup-2026/games/128144-GER-KOR")
                        .type(InternationalResultSourceType.OFFICIAL)
                        .build());
        return new Update()
                .set("status", InternationalResultStatus.FINAL)
                .set("participationStatus", ParticipationStatus.CONFIRMED)
                .set("teamResult", "B조 3위 · 8강 진출 결정전")
                .set("teamRecord", "1승 3패")
                .set("gamesPlayed", 4)
                .set("minutesPerGame", "17:33")
                .set("pointsPerGame", 8.0)
                .set("reboundsPerGame", 1.0)
                .set("assistsPerGame", 1.0)
                .set("stealsPerGame", 1.8)
                .set("fieldGoalPercent", 40.0)
                .set("threePointPercent", 38.5)
                .set("freeThrowPercent", 100.0)
                .set("highlight", "4경기에 출전해 헝가리전에서 12점, 독일과의 8강 진출 결정전에서 3점과 1스틸을 기록했습니다.")
                .set("statsUpdatedThrough", null)
                .set("sources", sources)
                .set("updatedAt", now);
    }
}
