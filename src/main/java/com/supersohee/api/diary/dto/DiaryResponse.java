package com.supersohee.api.diary.dto;

import com.supersohee.api.common.enums.GameResultType;
import com.supersohee.api.common.enums.WatchType;
import com.supersohee.api.diary.domain.Diary;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class DiaryResponse {

    private String id;
    private String userId;

    // 경기 정보
    private String gameId;
    private String date;
    private String time;
    private String location;
    private WatchType watchType;

    // MVP
    private String mvpPlayerName;
    private String mvpReason;

    // 선수 스탯
    private List<PlayerStatRecordResponse> playerStats;

    // Highlights
    private DiaryHighlightsResponse highlights;

    // 경기 결과
    private String gameResult;
    private Integer gameHomeScore;
    private Integer gameAwayScore;
    private GameResultType gameWinner;

    // 동행자 & 좌석
    private String companions;
    private List<String> companion;
    private String seat;
    private String seatId;

    // 사진 & 메모
    private List<String> photoUrls;
    private String memo;
    private String content; // 기존 필드 유지 (하위 호환성)

    // 하위 호환성을 위한 기존 필드
    private String cheeredPlayerName;
    private Integer cheeredPlayerPoints;
    private Integer cheeredPlayerAssists;
    private Integer cheeredPlayerRebounds;
    private Integer cheeredPlayerTwoPointMade;
    private Double cheeredPlayerTwoPointPercent;
    private Integer cheeredPlayerThreePointMade;
    private Double cheeredPlayerThreePointPercent;
    private Integer cheeredPlayerFreeThrowMade;
    private Double cheeredPlayerFreeThrowPercent;
    private Integer cheeredPlayerFouls;
    private Integer cheeredPlayerBlocks;
    private Integer cheeredPlayerTurnovers;
    private String cheeredPlayerMemo;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // ===== 임베디드 Response 클래스 =====

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PlayerStatRecordResponse {
        private String playerName;
        private String team;
        private PlayerStatsResponse stats;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PlayerStatsResponse {
        private Integer pts;
        private Integer fg2Made;
        private Integer fg2Att;
        private Integer fg3Made;
        private Integer fg3Att;
        private Integer rebOff;
        private Integer rebDef;
        private Integer ast;
        private Integer stl;
        private Integer blk;
        private Integer to;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DiaryHighlightsResponse {
        private Boolean overtime;
        private Boolean injury;
        private Boolean referee;
        private Boolean bestMood;
        private Boolean worstMood;
        private String custom;
    }

    public static DiaryResponse from(Diary diary) {
        // PlayerStats 변환
        List<PlayerStatRecordResponse> playerStatsResponse = null;
        if (diary.getPlayerStats() != null) {
            playerStatsResponse = diary.getPlayerStats().stream()
                    .map(record -> PlayerStatRecordResponse.builder()
                            .playerName(record.getPlayerName())
                            .team(record.getTeam())
                            .stats(record.getStats() != null ? PlayerStatsResponse.builder()
                                    .pts(record.getStats().getPts())
                                    .fg2Made(record.getStats().getFg2Made())
                                    .fg2Att(record.getStats().getFg2Att())
                                    .fg3Made(record.getStats().getFg3Made())
                                    .fg3Att(record.getStats().getFg3Att())
                                    .rebOff(record.getStats().getRebOff())
                                    .rebDef(record.getStats().getRebDef())
                                    .ast(record.getStats().getAst())
                                    .stl(record.getStats().getStl())
                                    .blk(record.getStats().getBlk())
                                    .to(record.getStats().getTo())
                                    .build() : null)
                            .build())
                    .collect(Collectors.toList());
        }

        // Highlights 변환
        DiaryHighlightsResponse highlightsResponse = null;
        if (diary.getHighlights() != null) {
            highlightsResponse = DiaryHighlightsResponse.builder()
                    .overtime(diary.getHighlights().getOvertime())
                    .injury(diary.getHighlights().getInjury())
                    .referee(diary.getHighlights().getReferee())
                    .bestMood(diary.getHighlights().getBestMood())
                    .worstMood(diary.getHighlights().getWorstMood())
                    .custom(diary.getHighlights().getCustom())
                    .build();
        }

        return DiaryResponse.builder()
                .id(diary.getId())
                .userId(diary.getUserId())
                .gameId(diary.getGameId())
                .date(diary.getDate())
                .time(diary.getTime())
                .location(diary.getLocation())
                .watchType(diary.getWatchType())
                .mvpPlayerName(diary.getMvpPlayerName())
                .mvpReason(diary.getMvpReason())
                .playerStats(playerStatsResponse)
                .highlights(highlightsResponse)
                .gameResult(diary.getGameResult())
                .gameHomeScore(diary.getGameHomeScore())
                .gameAwayScore(diary.getGameAwayScore())
                .gameWinner(diary.getGameWinner())
                .companions(diary.getCompanions())
                .companion(diary.getCompanion())
                .seat(diary.getSeat())
                .seatId(diary.getSeatId())
                .photoUrls(diary.getPhotoUrls())
                .memo(diary.getMemo())
                .content(diary.getContent())
                .cheeredPlayerName(diary.getCheeredPlayerName())
                .cheeredPlayerPoints(diary.getCheeredPlayerPoints())
                .cheeredPlayerAssists(diary.getCheeredPlayerAssists())
                .cheeredPlayerRebounds(diary.getCheeredPlayerRebounds())
                .cheeredPlayerTwoPointMade(diary.getCheeredPlayerTwoPointMade())
                .cheeredPlayerTwoPointPercent(diary.getCheeredPlayerTwoPointPercent())
                .cheeredPlayerThreePointMade(diary.getCheeredPlayerThreePointMade())
                .cheeredPlayerThreePointPercent(diary.getCheeredPlayerThreePointPercent())
                .cheeredPlayerFreeThrowMade(diary.getCheeredPlayerFreeThrowMade())
                .cheeredPlayerFreeThrowPercent(diary.getCheeredPlayerFreeThrowPercent())
                .cheeredPlayerFouls(diary.getCheeredPlayerFouls())
                .cheeredPlayerBlocks(diary.getCheeredPlayerBlocks())
                .cheeredPlayerTurnovers(diary.getCheeredPlayerTurnovers())
                .cheeredPlayerMemo(diary.getCheeredPlayerMemo())
                .createdAt(diary.getCreatedAt())
                .updatedAt(diary.getUpdatedAt())
                .build();
    }
}
