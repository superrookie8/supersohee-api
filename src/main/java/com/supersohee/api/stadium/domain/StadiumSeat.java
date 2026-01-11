package com.supersohee.api.stadium.domain;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Document(collection = "stadium_seats")
public class StadiumSeat {

    @Id
    private String id;

    private String stadiumId;      // Stadium.id 참조
    
    private String zoneName;        // 구역명 (예: "1층 flex석", "2층 D구역", "원정석 A구역")
    private String blockName;       // 블럭명 (예: "T블럭", "L블럭") - 선택적, null 허용
    
    // 개별 좌석 정보
    private String row;            // 열 (예: "A열", "3열", "1열") - 경기장마다 명칭 다를 수 있음
    private String number;         // 번호 (예: "1번", "15번", "19번")
    
    private String seatType;        // 좌석 타입 (예: "flex석", "테이블석", "일반석", "응원석", "원정석")
    
    private String floor;          // 층 정보 (예: "1층", "2층") - 선택적, null 허용
    private String description;    // 상세 설명 - 선택적, null 허용
    
}
