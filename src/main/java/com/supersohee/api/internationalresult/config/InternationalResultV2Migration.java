package com.supersohee.api.internationalresult.config;

import com.supersohee.api.internationalresult.domain.InternationalResult;
import com.supersohee.api.internationalresult.domain.InternationalResultCategory;
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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Profile("!test")
@Order(200)
@ConditionalOnProperty(
        prefix = "app.international-results-migration",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true)
public class InternationalResultV2Migration implements ApplicationRunner {
    static final String MIGRATION_ID = "international-results-v2";
    static final String MIGRATION_COLLECTION = "data_migrations";
    static final String COMPETITION_KEY = "2023-william-jones-cup";

    private final MongoOperations mongoOperations;

    @Override
    public void run(ApplicationArguments args) {
        try {
            Query marker = Query.query(Criteria.where("_id").is(MIGRATION_ID));
            if (mongoOperations.exists(marker, MIGRATION_COLLECTION)) {
                return;
            }

            LocalDateTime now = LocalDateTime.now();
            mongoOperations.upsert(
                    Query.query(Criteria.where("competitionKey").is(COMPETITION_KEY)),
                    insertOnly(now),
                    InternationalResult.class);

            mongoOperations.upsert(
                    marker,
                    new Update().setOnInsert("appliedAt", LocalDateTime.now()),
                    MIGRATION_COLLECTION);
        } catch (RuntimeException failure) {
            // Mongo diagnostics can contain stored field values. Do not attach the
            // provider exception when failing startup.
            throw new IllegalStateException("International result migration v2 could not be completed.");
        }
    }

    static Update insertOnly(LocalDateTime now) {
        List<InternationalResultSource> sources = List.of(
                InternationalResultSource.builder()
                        .label("WKBL 제42회 윌리엄 존스컵 일정")
                        .url("https://www.wkbl.or.kr/news/teamstory_view.asp?num=36969&page=490")
                        .type(InternationalResultSourceType.OFFICIAL)
                        .build(),
                InternationalResultSource.builder()
                        .label("연합뉴스 제42회 윌리엄 존스컵 결과")
                        .url("https://www.yna.co.kr/view/AKR20230809163500007")
                        .type(InternationalResultSourceType.SECONDARY)
                        .build());
        return new Update()
                .setOnInsert("competitionKey", COMPETITION_KEY)
                .setOnInsert("competitionName", "제42회 윌리엄 존스컵")
                .setOnInsert("editionLabel", "2023")
                .setOnInsert("category", InternationalResultCategory.CLUB)
                .setOnInsert("status", InternationalResultStatus.FINAL)
                .setOnInsert("participationStatus", ParticipationStatus.CONFIRMED)
                .setOnInsert("startDate", LocalDate.of(2023, 8, 5))
                .setOnInsert("endDate", LocalDate.of(2023, 8, 9))
                .setOnInsert("location", "대만 타이베이")
                .setOnInsert("teamName", "부산 BNK 썸")
                .setOnInsert("teamResult", "준우승")
                .setOnInsert("teamRecord", "4승 1패")
                .setOnInsert("gamesPlayed", 5)
                .setOnInsert("minutesPerGame", null)
                .setOnInsert("pointsPerGame", 18.2)
                .setOnInsert("reboundsPerGame", null)
                .setOnInsert("assistsPerGame", null)
                .setOnInsert("stealsPerGame", null)
                .setOnInsert("fieldGoalPercent", null)
                .setOnInsert("threePointPercent", null)
                .setOnInsert("freeThrowPercent", null)
                .setOnInsert("highlight", "최종전에서 31점·6리바운드를 기록하고 대회 Best Five에 선정됐습니다.")
                .setOnInsert("statsUpdatedThrough", null)
                .setOnInsert("sources", sources)
                .setOnInsert("published", true)
                .setOnInsert("displayOrder", 75)
                .setOnInsert("createdAt", now)
                .setOnInsert("updatedAt", now);
    }
}
