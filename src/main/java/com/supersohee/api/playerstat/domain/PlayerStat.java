package com.supersohee.api.playerstat.domain;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Getter
@Builder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
@Document(collection = "player_stats")
public class PlayerStat {

    @Id
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
}