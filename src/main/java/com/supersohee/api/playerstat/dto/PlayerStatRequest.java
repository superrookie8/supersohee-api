package com.supersohee.api.playerstat.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class PlayerStatRequest {

    private String season; // 2025-2026
    private String team; // 팀명 (BNK 썸, OK저축은행 등)

    // ========== 통산평균기록 ==========
    private Integer gamesPlayed; // G
    private String minutesPerGame; // MPG (예: "35:00")

    // 슛 성공률
    private Double twoPointPercent; // 2P%
    private Double threePointPercent; // 3P%
    private Double freeThrowPercent; // FT%

    // 리바운드 (평균)
    private Double offensiveRebounds; // OFF
    private Double defensiveRebounds; // DEF
    private Double totalRebounds; // TOT

    // 기타 스탯 (평균)
    private Double ppg; // PPG
    private Double apg; // APG
    private Double spg; // SPG
    private Double bpg; // BPG
    private Double turnovers; // TO
    private Double fouls; // PF

    // ========== 통산합계기록 ==========
    private String totalMinutes; // MIN (예: "440:28")

    // 슛 시도/성공
    private Integer twoPointMade; // 2점슛 성공
    private Integer twoPointAttempted; // 2점슛 시도
    private Integer threePointMade; // 3점슛 성공
    private Integer threePointAttempted; // 3점슛 시도
    private Integer freeThrowMade; // 자유투 성공
    private Integer freeThrowAttempted; // 자유투 시도

    // 리바운드 (합계)
    private Integer totalOffensiveRebounds; // OFF 총합
    private Integer totalDefensiveRebounds; // DEF 총합
    private Integer totalTotalRebounds; // TOT 총합

    // 기타 스탯 (합계)
    private Integer totalAssists; // AST 총합
    private Integer totalSteals; // STL 총합
    private Integer totalBlocks; // BLK 총합
    private Integer totalTurnovers; // TO 총합
    private Integer totalFouls; // PF 총합
    private Integer totalPoints; // PTS 총합

    // Jackson이 생성자 기반 바인딩을 사용하도록 @JsonCreator 사용
    @JsonCreator
    public PlayerStatRequest(
            @JsonProperty("season") String season,
            @JsonProperty("team") String team,
            @JsonProperty("gamesPlayed") Integer gamesPlayed,
            @JsonProperty("minutesPerGame") String minutesPerGame,
            @JsonProperty("twoPointPercent") Double twoPointPercent,
            @JsonProperty("threePointPercent") Double threePointPercent,
            @JsonProperty("freeThrowPercent") Double freeThrowPercent,
            @JsonProperty("offensiveRebounds") Double offensiveRebounds,
            @JsonProperty("defensiveRebounds") Double defensiveRebounds,
            @JsonProperty("totalRebounds") Double totalRebounds,
            @JsonProperty("ppg") Double ppg,
            @JsonProperty("apg") Double apg,
            @JsonProperty("spg") Double spg,
            @JsonProperty("bpg") Double bpg,
            @JsonProperty("turnovers") Double turnovers,
            @JsonProperty("fouls") Double fouls,
            @JsonProperty("totalMinutes") String totalMinutes,
            @JsonProperty("twoPointMade") Integer twoPointMade,
            @JsonProperty("twoPointAttempted") Integer twoPointAttempted,
            @JsonProperty("threePointMade") Integer threePointMade,
            @JsonProperty("threePointAttempted") Integer threePointAttempted,
            @JsonProperty("freeThrowMade") Integer freeThrowMade,
            @JsonProperty("freeThrowAttempted") Integer freeThrowAttempted,
            @JsonProperty("totalOffensiveRebounds") Integer totalOffensiveRebounds,
            @JsonProperty("totalDefensiveRebounds") Integer totalDefensiveRebounds,
            @JsonProperty("totalTotalRebounds") Integer totalTotalRebounds,
            @JsonProperty("totalAssists") Integer totalAssists,
            @JsonProperty("totalSteals") Integer totalSteals,
            @JsonProperty("totalBlocks") Integer totalBlocks,
            @JsonProperty("totalTurnovers") Integer totalTurnovers,
            @JsonProperty("totalFouls") Integer totalFouls,
            @JsonProperty("totalPoints") Integer totalPoints) {
        this.season = season;
        this.team = team;
        this.gamesPlayed = gamesPlayed;
        this.minutesPerGame = minutesPerGame;
        this.twoPointPercent = twoPointPercent;
        this.threePointPercent = threePointPercent;
        this.freeThrowPercent = freeThrowPercent;
        this.offensiveRebounds = offensiveRebounds;
        this.defensiveRebounds = defensiveRebounds;
        this.totalRebounds = totalRebounds;
        this.ppg = ppg;
        this.apg = apg;
        this.spg = spg;
        this.bpg = bpg;
        this.turnovers = turnovers;
        this.fouls = fouls;
        this.totalMinutes = totalMinutes;
        this.twoPointMade = twoPointMade;
        this.twoPointAttempted = twoPointAttempted;
        this.threePointMade = threePointMade;
        this.threePointAttempted = threePointAttempted;
        this.freeThrowMade = freeThrowMade;
        this.freeThrowAttempted = freeThrowAttempted;
        this.totalOffensiveRebounds = totalOffensiveRebounds;
        this.totalDefensiveRebounds = totalDefensiveRebounds;
        this.totalTotalRebounds = totalTotalRebounds;
        this.totalAssists = totalAssists;
        this.totalSteals = totalSteals;
        this.totalBlocks = totalBlocks;
        this.totalTurnovers = totalTurnovers;
        this.totalFouls = totalFouls;
        this.totalPoints = totalPoints;
    }
}
