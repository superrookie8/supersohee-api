package com.supersohee.api.schedule.dto;

import com.supersohee.api.schedule.domain.Schedule;
import com.supersohee.api.stadium.domain.Stadium;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScheduleDetailsResponse {

    // 기본 스케줄 정보
    private String id;
    private String title;
    private String description;
    private LocalDateTime startDateTime;
    private LocalDateTime endDateTime;
    private String location;
    private String type;
    private String color;
    private String url;
    private Boolean isActive;

    // 경기장 정보 (stadiumId가 있을 때만 포함)
    private StadiumInfo stadium;

    // 직관일지 바로가기용 gameId
    private String gameId;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StadiumInfo {
        private String id;
        private String name;
        private String address;
        private Integer capacity;
        private Double latitude; // 지도 좌표
        private Double longitude; // 지도 좌표
        private String imageUrl;
        private List<String> subwayInfo; // 교통정보: 지하철
        private List<String> busInfo; // 교통정보: 버스
        private String intercityRoute; // 교통정보: 시외교통
    }

    public static ScheduleDetailsResponse from(Schedule schedule, Stadium stadium, String gameId) {
        ScheduleDetailsResponse.ScheduleDetailsResponseBuilder builder = ScheduleDetailsResponse.builder()
                .id(schedule.getId())
                .title(schedule.getTitle())
                .description(schedule.getDescription())
                .startDateTime(schedule.getStartDateTime())
                .endDateTime(schedule.getEndDateTime())
                .location(schedule.getLocation())
                .type(schedule.getType())
                .color(schedule.getColor())
                .url(schedule.getUrl())
                .isActive(schedule.getIsActive())
                .gameId(gameId)
                .createdAt(schedule.getCreatedAt())
                .updatedAt(schedule.getUpdatedAt());

        // 경기장 정보가 있으면 포함
        if (stadium != null) {
            builder.stadium(StadiumInfo.builder()
                    .id(stadium.getId())
                    .name(stadium.getName())
                    .address(stadium.getAddress())
                    .capacity(stadium.getCapacity())
                    .latitude(stadium.getLatitude())
                    .longitude(stadium.getLongitude())
                    .imageUrl(stadium.getImageUrl())
                    .subwayInfo(stadium.getSubwayInfo())
                    .busInfo(stadium.getBusInfo())
                    .intercityRoute(stadium.getIntercityRoute())
                    .build());
        }

        return builder.build();
    }
}
