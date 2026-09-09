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
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.index.Index;
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
@Order(100)
@ConditionalOnProperty(
        prefix = "app.international-results-migration",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true)
public class InternationalResultV1Migration implements ApplicationRunner {
    static final String MIGRATION_ID = "international-results-v1";
    static final String MIGRATION_COLLECTION = "data_migrations";
    static final String UNIQUE_INDEX_NAME = "international_result_competition_key_unique";

    private final MongoOperations mongoOperations;

    @Override
    public void run(ApplicationArguments args) {
        try {
            ensureUniqueIndex();
            Query marker = Query.query(Criteria.where("_id").is(MIGRATION_ID));
            if (mongoOperations.exists(marker, MIGRATION_COLLECTION)) {
                return;
            }

            LocalDateTime now = LocalDateTime.now();
            for (InternationalResult seed : seeds(now)) {
                insertIfAbsent(seed);
            }

            mongoOperations.upsert(marker,
                    new Update().setOnInsert("appliedAt", LocalDateTime.now()),
                    MIGRATION_COLLECTION);
        } catch (RuntimeException failure) {
            // Mongo diagnostics can include stored field values. Fail startup without
            // attaching the provider exception to application logs.
            throw new IllegalStateException("International result migration v1 could not be completed.");
        }
    }

    private void ensureUniqueIndex() {
        mongoOperations.indexOps(InternationalResult.class).createIndex(
                new Index().on("competitionKey", Sort.Direction.ASC)
                        .unique()
                        .named(UNIQUE_INDEX_NAME));
    }

    private void insertIfAbsent(InternationalResult seed) {
        Query identity = Query.query(Criteria.where("competitionKey").is(seed.getCompetitionKey()));
        Update update = new Update()
                .setOnInsert("competitionKey", seed.getCompetitionKey())
                .setOnInsert("competitionName", seed.getCompetitionName())
                .setOnInsert("editionLabel", seed.getEditionLabel())
                .setOnInsert("category", seed.getCategory())
                .setOnInsert("status", seed.getStatus())
                .setOnInsert("participationStatus", seed.getParticipationStatus())
                .setOnInsert("startDate", seed.getStartDate())
                .setOnInsert("endDate", seed.getEndDate())
                .setOnInsert("location", seed.getLocation())
                .setOnInsert("teamName", seed.getTeamName())
                .setOnInsert("teamResult", seed.getTeamResult())
                .setOnInsert("teamRecord", seed.getTeamRecord())
                .setOnInsert("gamesPlayed", seed.getGamesPlayed())
                .setOnInsert("minutesPerGame", seed.getMinutesPerGame())
                .setOnInsert("pointsPerGame", seed.getPointsPerGame())
                .setOnInsert("reboundsPerGame", seed.getReboundsPerGame())
                .setOnInsert("assistsPerGame", seed.getAssistsPerGame())
                .setOnInsert("stealsPerGame", seed.getStealsPerGame())
                .setOnInsert("fieldGoalPercent", seed.getFieldGoalPercent())
                .setOnInsert("threePointPercent", seed.getThreePointPercent())
                .setOnInsert("freeThrowPercent", seed.getFreeThrowPercent())
                .setOnInsert("highlight", seed.getHighlight())
                .setOnInsert("statsUpdatedThrough", seed.getStatsUpdatedThrough())
                .setOnInsert("sources", seed.getSources())
                .setOnInsert("published", seed.isPublished())
                .setOnInsert("displayOrder", seed.getDisplayOrder())
                .setOnInsert("createdAt", seed.getCreatedAt())
                .setOnInsert("updatedAt", seed.getUpdatedAt());
        mongoOperations.upsert(identity, update, InternationalResult.class);
    }

    static List<InternationalResult> seeds(LocalDateTime now) {
        String fibaProfile = "https://www.fiba.basketball/en/players/219255-sohee-lee";
        return List.of(
                seed("2026-aichi-nagoya-asian-games", "아이치·나고야 아시안게임 여자농구", "2026",
                        InternationalResultCategory.NATIONAL_TEAM, InternationalResultStatus.SCHEDULED,
                        ParticipationStatus.UNCONFIRMED, "2026-09-17", "2026-09-26", "일본 아이치·나고야",
                        "대한민국", null, null, null, null, null, null, null, null, null, null, null,
                        "공식 최종 명단 발표 후 출전 여부를 갱신합니다.", null,
                        official("대한민국농구협회 2026 아이치·나고야 아시안게임 일정",
                                "https://www.koreabasketball.or.kr/game/intl_schedule_view.php?idx=1352"),
                        10, now),
                seed("2026-fiba-womens-world-cup", "FIBA 여자농구 월드컵", "2026",
                        InternationalResultCategory.NATIONAL_TEAM, InternationalResultStatus.FINAL,
                        ParticipationStatus.CONFIRMED, "2026-09-04", "2026-09-13", "독일 베를린",
                        "대한민국", "B조 3위 · 8강 진출 결정전", "1승 3패", 4, "17:33",
                        8.0, 1.0, 1.0, 1.8, 40.0, 38.5, 100.0,
                        "4경기에 출전해 헝가리전에서 12점, 독일과의 8강 진출 결정전에서 3점과 1스틸을 기록했습니다.", null,
                        List.of(
                                source("FIBA 2026 월드컵 이소희 선수 기록",
                                        "https://www.fiba.basketball/en/events/fiba-womens-basketball-world-cup-2026/teams/korea/219255-sohee-lee"),
                                source("FIBA 2026 월드컵 독일-대한민국 공식 경기",
                                        "https://www.fiba.basketball/en/events/fiba-womens-basketball-world-cup-2026/games/128144-GER-KOR")),
                        20, now),
                seed("2026-mitsui-fudosan-cup-tokyo", "미쓰이 후도산컵 도쿄대회", "2026",
                        InternationalResultCategory.EXHIBITION, InternationalResultStatus.FINAL,
                        ParticipationStatus.CONFIRMED, "2026-08-13", "2026-08-14", "일본 도쿄",
                        "대한민국", "2경기 평가전", "0승 2패", 2, "27:18", 10.0, 2.0, 1.5, 0.5,
                        53.8, 37.5, 75.0, "1차전 13점, 2차전 7점을 기록했습니다.", null,
                        List.of(
                                source("일본농구협회 1차전 공식 박스스코어",
                                        "https://akatsukijapan-women-2026.japanbasketball.jp/wp-content/uploads/boxscore/3.pdf"),
                                source("일본농구협회 2차전 공식 박스스코어",
                                        "https://akatsukijapan-women-2026.japanbasketball.jp/wp-content/uploads/boxscore/4.pdf")),
                        30, now),
                seed("2026-fiba-world-cup-qualifying-tournament", "FIBA 여자농구 월드컵 최종예선", "2026",
                        InternationalResultCategory.NATIONAL_TEAM, InternationalResultStatus.FINAL,
                        ParticipationStatus.CONFIRMED, "2026-03-11", "2026-03-17", "프랑스 리옹·빌뢰르반",
                        "대한민국", "3위", "3승 2패", 4, "13:24", 6.3, 1.0, 1.3, null, 39.1, 35.7, 100.0,
                        "나이지리아전을 제외한 4경기에 출전했습니다.", null,
                        official("FIBA 2026 월드컵 최종예선 이소희 선수 기록",
                                "https://www.fiba.basketball/en/events/fiba-womens-basketball-world-cup-2026-qualifying-tournament-villeurbanne-france/teams/korea/219255-sohee-lee"),
                        40, now),
                seed("2025-fiba-womens-basketball-league-asia", "FIBA Women's Basketball League Asia", "2025",
                        InternationalResultCategory.CLUB, InternationalResultStatus.FINAL,
                        ParticipationStatus.CONFIRMED, "2025-09-23", "2025-09-28", "중국 둥관",
                        "부산 BNK 썸", "3위", "2승 2패", 4, "25:30", 18.5, 2.5, 2.0, 1.8, 45.6, 42.5, 83.3,
                        "대회 득점 4위. 3위 결정전에서 30점과 3점슛 6개를 기록했습니다.", null,
                        official("FIBA WBL Asia 2025 이소희 선수 기록",
                                "https://www.fiba.basketball/en/events/fiba-womens-basketball-league-asia-2025/teams/bnk-sum/219255-sohee-lee"),
                        50, now),
                seed("2026-fiba-world-cup-pre-qualifying-tournament", "FIBA 여자농구 월드컵 사전예선", "2026",
                        InternationalResultCategory.NATIONAL_TEAM, InternationalResultStatus.FINAL,
                        ParticipationStatus.CONFIRMED, "2024-08-19", "2024-08-25", "멕시코 멕시코시티",
                        "대한민국", "준우승", null, 5, "19:06", 8.8, 3.8, 1.8, 1.2, 45.7, 46.7, 83.3,
                        "몬테네그로와의 준결승에서 23점과 6리바운드를 기록했습니다.", null,
                        official("FIBA 2026 월드컵 사전예선 이소희 선수 기록",
                                "https://www.fiba.basketball/en/events/fiba-womens-basketball-world-cup-2026-pre-qualifying-tournament-mexico-city-mexico/teams/korea/219255-sohee-lee"),
                        60, now),
                seed("2022-hangzhou-asian-games", "제19회 항저우 아시안게임 여자농구", "2022 항저우",
                        InternationalResultCategory.NATIONAL_TEAM, InternationalResultStatus.FINAL,
                        ParticipationStatus.CONFIRMED, "2023-09-23", "2023-10-08", "중국 항저우",
                        "대한민국", "동메달", "5승 1패", 6, "15:20", 6.0, 3.2, 1.7, null, 36.0, 35.0, 71.0,
                        "동메달 결정전에서 북한을 93-63으로 꺾었습니다.", null,
                        official("OCA 항저우 아시안게임 Basketball Results Book",
                                "https://www.ocagames.com/orb/books/Hangzhou_2022/Basketball.pdf"),
                        70, now),
                seed("2023-fiba-womens-asia-cup-division-a", "FIBA 여자 아시아컵 Division A", "2023",
                        InternationalResultCategory.NATIONAL_TEAM, InternationalResultStatus.FINAL,
                        ParticipationStatus.CONFIRMED, "2023-06-26", "2023-07-02", "호주 시드니",
                        "대한민국", "5위", null, 4, "19:06", 6.0, 1.3, 1.8, 1.3, 26.5, 26.1, null,
                        "중국전을 제외한 4경기에 출전했습니다.", null,
                        official("FIBA 이소희 선수 국제대회 프로필", fibaProfile), 80, now),
                seed("2022-fiba-womens-world-cup", "FIBA 여자농구 월드컵", "2022",
                        InternationalResultCategory.NATIONAL_TEAM, InternationalResultStatus.FINAL,
                        ParticipationStatus.CONFIRMED, "2022-09-22", "2022-10-01", "호주 시드니",
                        "대한민국", "10위", null, 5, "06:24", 3.8, 1.2, 0.2, null, 53.3, 42.9, null,
                        "성인 월드컵 본선 첫 출전 대회입니다.", null,
                        official("FIBA 이소희 선수 국제대회 프로필", fibaProfile), 90, now),
                seed("2022-fiba-world-cup-qualifying-tournament", "FIBA 여자농구 월드컵 최종예선", "2022",
                        InternationalResultCategory.NATIONAL_TEAM, InternationalResultStatus.FINAL,
                        ParticipationStatus.CONFIRMED, "2022-02-10", "2022-02-13", "세르비아 베오그라드",
                        "대한민국", "3위", null, 1, "20:00", 5.0, 1.0, 0.0, 1.0, null, null, null,
                        "호주전에서 20분 동안 5점, 1리바운드, 1스틸을 기록했습니다.", null,
                        official("FIBA 이소희 선수 국제대회 프로필", fibaProfile), 100, now));
    }

    private static InternationalResult seed(
            String key, String name, String edition, InternationalResultCategory category,
            InternationalResultStatus status, ParticipationStatus participationStatus,
            String start, String end, String location, String teamName, String teamResult, String teamRecord,
            Integer games, String minutes, Double pointsPerGame, Double reboundsPerGame,
            Double assistsPerGame, Double stealsPerGame,
            Double fieldGoalPercent, Double threePointPercent, Double freeThrowPercent,
            String highlight, String statsUpdatedThrough, List<InternationalResultSource> sources,
            int displayOrder, LocalDateTime now) {
        return InternationalResult.builder()
                .competitionKey(key).competitionName(name).editionLabel(edition).category(category).status(status)
                .participationStatus(participationStatus).startDate(LocalDate.parse(start)).endDate(LocalDate.parse(end))
                .location(location).teamName(teamName).teamResult(teamResult).teamRecord(teamRecord)
                .gamesPlayed(games).minutesPerGame(minutes).pointsPerGame(pointsPerGame)
                .reboundsPerGame(reboundsPerGame).assistsPerGame(assistsPerGame).stealsPerGame(stealsPerGame)
                .fieldGoalPercent(fieldGoalPercent).threePointPercent(threePointPercent)
                .freeThrowPercent(freeThrowPercent).highlight(highlight)
                .statsUpdatedThrough(statsUpdatedThrough == null ? null : LocalDate.parse(statsUpdatedThrough))
                .sources(sources).published(true).displayOrder(displayOrder).createdAt(now).updatedAt(now).build();
    }

    private static List<InternationalResultSource> official(String label, String url) {
        return List.of(source(label, url));
    }

    private static InternationalResultSource source(String label, String url) {
        return InternationalResultSource.builder()
                .label(label).url(url).type(InternationalResultSourceType.OFFICIAL).build();
    }
}
