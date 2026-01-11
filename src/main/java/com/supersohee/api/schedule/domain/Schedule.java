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
    
    // 노출 여부
    private Boolean isActive;
}
