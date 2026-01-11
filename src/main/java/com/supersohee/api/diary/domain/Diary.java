package com.supersohee.api.diary.domain;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import com.supersohee.api.common.BaseDocument;
import com.supersohee.api.common.enums.WatchType;
import com.supersohee.api.common.enums.GameResultType;
import java.util.List;


@Getter
@Builder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
@Document(collection = "diaries")
public class Diary extends BaseDocument {

    @Id
    private String id;

    private String userId;           // User.id (필수)

    // 경기 정보 (선택적 gameId 참조 또는 직접 입력)
    private String gameId;           // Game.id (nullable)
    private String date;             // "2025-01-15" (nullable)
    private String time;             // "19:00" (nullable)
    private String location;         // "사직실내체육관" (nullable)

    private WatchType watchType;     // DIRECT | HOUSE (nullable)

    // MVP (모두 nullable)
    private String mvpPlayerName;     // 최우수선수 이름 (nullable)
    private String mvpReason;        // MVP 이유 (nullable, 새로 추가)

    // 선수 스탯 (배열, 확장 가능, 기본 1명)
    private List<PlayerStatRecord> playerStats; // nullable, 빈 배열 가능 (새로 추가)

    // Highlights (모두 nullable)
    private DiaryHighlights highlights; // nullable (새로 추가)

    // 경기 결과
    private String gameResult;       // "승" | "패" | null (무승부 없음, 새로 추가)
    private Integer gameHomeScore;   // 홈팀 점수 (nullable)
    private Integer gameAwayScore;   // 원정팀 점수 (nullable)
    private GameResultType gameWinner; // 승자 (HOME | AWAY, nullable)

    // 동행자 & 좌석
    private String companions;       // 동행자 문자열 (nullable, 새로 추가)
    private List<String> companion;  // 동행자 User.id 리스트 (nullable, 기존 유지)
    private String seat;             // 좌석 문자열 (nullable, 새로 추가)
    private String seatId;           // StadiumSeat.id 참조 (nullable, 기존 유지)

    // 사진 & 메모
    private List<String> photoUrls;  // nullable, 빈 배열 가능
    private String memo;             // 일지 메모 (nullable, 새로 추가)
    private String content;          // 기존 필드 유지 (하위 호환성)

    // 하위 호환성을 위한 기존 필드 (선택적 사용)
    private String cheeredPlayerName; // 응원한 선수 이름 (nullable)
    private Integer cheeredPlayerPoints;              // 득점 (nullable)
    private Integer cheeredPlayerAssists;             // 어시스트 (nullable)
    private Integer cheeredPlayerRebounds;             // 리바운드 (nullable)
    private Integer cheeredPlayerTwoPointMade;         // 2점 성공 (nullable)
    private Double cheeredPlayerTwoPointPercent;      // 2점 성공률 (nullable)
    private Integer cheeredPlayerThreePointMade;       // 3점 성공 (nullable)
    private Double cheeredPlayerThreePointPercent;    // 3점 성공률 (nullable)
    private Integer cheeredPlayerFreeThrowMade;       // 자유투 성공 (nullable)
    private Double cheeredPlayerFreeThrowPercent;     // 자유투 성공률 (nullable)
    private Integer cheeredPlayerFouls;                // 파울 개수 (nullable)
    private Integer cheeredPlayerBlocks;               // 블락 (nullable)
    private Integer cheeredPlayerTurnovers;            // 턴오버 개수 (nullable)
    private String cheeredPlayerMemo;                  // 응원 선수에 대한 메모 (nullable)

    // ===== 임베디드 클래스 =====

    /**
     * 선수 스탯 기록 (확장 가능한 구조, 기본 1명)
     */
    @Getter
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class PlayerStatRecord {
        private String playerName;   // 선수 이름 (nullable)
        private String team;          // 팀명 (nullable)
        private PlayerStats stats;    // 스탯 정보 (nullable)
    }

    /**
     * 선수 스탯 상세 정보
     */
    @Getter
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class PlayerStats {
        private Integer pts;          // 득점 (nullable)
        private Integer fg2Made;      // 2점 성공 (nullable)
        private Integer fg2Att;      // 2점 시도 (nullable)
        private Integer fg3Made;      // 3점 성공 (nullable)
        private Integer fg3Att;      // 3점 시도 (nullable)
        private Integer rebOff;      // 공격 리바운드 (nullable)
        private Integer rebDef;      // 수비 리바운드 (nullable)
        private Integer ast;         // 어시스트 (nullable)
        private Integer stl;         // 스틸 (nullable)
        private Integer blk;         // 블락 (nullable)
        private Integer to;          // 턴오버 (nullable)
    }

    /**
     * 경기 특이사항 (Highlights)
     */
    @Getter
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class DiaryHighlights {
        private Boolean overtime;    // 연장전 (nullable)
        private Boolean injury;       // 부상 이슈 (nullable)
        private Boolean referee;      // 판정 이슈 (nullable)
        private Boolean bestMood;    // 분위기 최고 (nullable)
        private Boolean worstMood;    // 분위기 최악 (nullable)
        private String custom;       // 기타 특이사항 (nullable)
    }
}

