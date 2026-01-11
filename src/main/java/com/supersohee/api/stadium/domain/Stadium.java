package com.supersohee.api.stadium.domain;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.util.List;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Document(collection = "stadiums")
public class Stadium {

    @Id
    private String id;

    private String name;
    private String address;
    private Integer capacity; // 수용인원 (예: 14099)

    private Double latitude;
    private Double longitude;

    private String imageUrl;

    private List<String> subwayInfo;
    private List<String> busInfo;
    private String intercityRoute; // 시외교통 경로 (KTX, 시외버스 등)
}
