package com.supersohee.api.playerstat.dto;

import com.supersohee.api.playerstat.domain.PlayerStat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class PlayerStatResponse {

    private String id;
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

    public static PlayerStatResponse from(PlayerStat playerStat) {
        return PlayerStatResponse.builder()
                .id(playerStat.getId())
                .season(playerStat.getSeason())
                .team(playerStat.getTeam())
                .gamesPlayed(playerStat.getGamesPlayed())
                .minutesPerGame(playerStat.getMinutesPerGame())
                .twoPointPercent(playerStat.getTwoPointPercent())
                .threePointPercent(playerStat.getThreePointPercent())
                .freeThrowPercent(playerStat.getFreeThrowPercent())
                .offensiveRebounds(playerStat.getOffensiveRebounds())
                .defensiveRebounds(playerStat.getDefensiveRebounds())
                .totalRebounds(playerStat.getTotalRebounds())
                .ppg(playerStat.getPpg())
                .apg(playerStat.getApg())
                .spg(playerStat.getSpg())
                .bpg(playerStat.getBpg())
                .turnovers(playerStat.getTurnovers())
                .fouls(playerStat.getFouls())
                .totalMinutes(playerStat.getTotalMinutes())
                .twoPointMade(playerStat.getTwoPointMade())
                .twoPointAttempted(playerStat.getTwoPointAttempted())
                .threePointMade(playerStat.getThreePointMade())
                .threePointAttempted(playerStat.getThreePointAttempted())
                .freeThrowMade(playerStat.getFreeThrowMade())
                .freeThrowAttempted(playerStat.getFreeThrowAttempted())
                .totalOffensiveRebounds(playerStat.getTotalOffensiveRebounds())
                .totalDefensiveRebounds(playerStat.getTotalDefensiveRebounds())
                .totalTotalRebounds(playerStat.getTotalTotalRebounds())
                .totalAssists(playerStat.getTotalAssists())
                .totalSteals(playerStat.getTotalSteals())
                .totalBlocks(playerStat.getTotalBlocks())
                .totalTurnovers(playerStat.getTotalTurnovers())
                .totalFouls(playerStat.getTotalFouls())
                .totalPoints(playerStat.getTotalPoints())
                .build();
    }
}
