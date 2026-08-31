package com.supersohee.api.schedule.domain;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import com.supersohee.api.common.BaseDocument;

import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Document(collection = "schedules")
public class Schedule extends BaseDocument {
    
    @Id
    private String id;
    
    private String title;              // 스케줄 제목
    private String description;        // 설명 (선택)
    private LocalDateTime startDateTime; // 시작 일시
    private LocalDateTime endDateTime;   // 종료 일시 (선택, 없으면 하루 종일)
    private String location;           // 위치 (선택)
    private String type;               // 타입: "game", "event", "other" 등
    private String color;              // 캘린더 표시 색상 (hex 코드, 예: "#FF5733")
    private String url;                // 관련 링크 (선택)
    
    // 경기장 및 경기 연결 (선택)
    private String stadiumId;         // 경기장 ID (Stadium.id 참조)
    private String gameId;             // 경기 ID (Game.id 참조, 직관일지 링크용)

    // 관리자 경기 일정 계약(기존 공개 필드에 대한 additive metadata)
    private String season;
    private String opponent;
    private Boolean isHome;
    private String extraHome;
    private Boolean specialGame;
    
    // 노출 여부
    private Boolean isActive;

    /**
     * 시즌 문자열을 결정한다.
     * season 필드가 없는 과거 문서(어드민 시즌 계약 이전에 적재된 경기)는
     * 경기 일자로부터 시즌을 유추한다. WKBL 시즌은 가을에 시작해 이듬해 봄에 끝나므로
     * 7월 이후 경기는 해당 연도 시작 시즌, 6월 이전 경기는 전년도 시작 시즌에 속한다.
     */
    public String resolveSeason() {
        if (season != null && !season.isBlank()) {
            return season;
        }
        if (startDateTime == null) {
            return null;
        }
        int year = startDateTime.getYear();
        return startDateTime.getMonthValue() >= 7
                ? year + "-" + (year + 1)
                : (year - 1) + "-" + year;
    }

    /** isHome 필드가 없는 과거 문서는 location("Home") 으로 홈 경기 여부를 판단한다. */
    public boolean resolveIsHome() {
        return isHome != null ? isHome : "Home".equals(location);
    }

    /** specialGame 필드가 없는 과거 문서(올스타전 등)는 type 으로 특별 경기 여부를 판단한다. */
    public boolean resolveSpecialGame() {
        return specialGame != null ? specialGame : "specialGame".equals(type);
    }
}
