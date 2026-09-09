package com.supersohee.api.internationalresult.config;

import com.mongodb.client.result.UpdateResult;
import com.supersohee.api.internationalresult.domain.InternationalResult;
import com.supersohee.api.internationalresult.domain.InternationalResultSource;
import com.supersohee.api.internationalresult.domain.InternationalResultSourceType;
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

import java.util.List;

@Component
@RequiredArgsConstructor
@Profile("!test")
@Order(400)
@ConditionalOnProperty(
        prefix = "app.international-results-migration",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true)
public class InternationalResultV4Migration implements ApplicationRunner {
    static final String MIGRATION_ID = "international-results-v4-world-cup-2026-highlight";
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
                    competitionQuery(), highlightUpdate(), InternationalResult.class);
            if (result.getMatchedCount() != 1) {
                throw new IllegalStateException("Expected one world cup result.");
            }

            mongoOperations.upsert(marker,
                    new Update().setOnInsert("appliedAt", java.time.LocalDateTime.now()),
                    MIGRATION_COLLECTION);
        } catch (RuntimeException failure) {
            // Mongo diagnostics can contain stored values. Do not expose the cause at startup.
            throw new IllegalStateException("International result migration v4 could not be completed.");
        }
    }

    static Query competitionQuery() {
        return Query.query(Criteria.where("competitionKey").is(COMPETITION_KEY));
    }

    static Update highlightUpdate() {
        List<InternationalResultSource> sources = List.of(
                InternationalResultSource.builder()
                        .label("FIBA 2026 월드컵 이소희 선수 기록")
                        .url("https://www.fiba.basketball/en/events/fiba-womens-basketball-world-cup-2026/teams/korea/219255-sohee-lee")
                        .type(InternationalResultSourceType.OFFICIAL)
                        .build(),
                InternationalResultSource.builder()
                        .label("FIBA 2026 월드컵 헝가리-대한민국 공식 경기")
                        .url("https://www.fiba.basketball/en/events/fiba-womens-basketball-world-cup-2026/games/128126-HUN-KOR")
                        .type(InternationalResultSourceType.OFFICIAL)
                        .build());
        return new Update()
                .set("highlight", "헝가리전에서 12점, 3어시스트와 2스틸을 기록하며 이번 대회 개인 최다 득점을 올렸습니다.")
                .set("sources", sources);
    }
}
