package com.supersohee.api.schedule.dto;

import com.supersohee.api.schedule.domain.Schedule;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScheduleResponse {
    
    private String id;
    private String competitionKey;
    private String competitionName;
    private String competitionKind;
    private String editionLabel;
    private String teamType;
    private String ourTeamName;
    private String venueType;
    private String venueName;
    private String stage;
    private String stadiumId;

    private String title;
    private String description;
    private LocalDateTime startDateTime;
    private LocalDateTime endDateTime;
    private String location;
    private String type;
    private String color;
    private String url;
    private Boolean isActive;

    private String opponent;
    /** 시즌(YYYY-YYYY). season 필드가 없는 과거 문서는 경기일로 유추한다. */
    private String season;
    private Boolean isHome;
    private Boolean specialGame;
    
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    public static ScheduleResponse from(Schedule schedule) {
        return ScheduleResponse.builder()
                .id(schedule.getId())
                .competitionKey(schedule.resolveCompetitionKey())
                .competitionName(schedule.resolveCompetitionName())
                .competitionKind(schedule.resolveCompetitionKind())
                .editionLabel(schedule.resolveEditionLabel())
                .teamType(schedule.resolveTeamType())
                .ourTeamName(schedule.resolveOurTeamName())
                .venueType(schedule.resolveVenueType())
                .venueName(schedule.resolveVenueName())
                .stage(schedule.getStage())
                .stadiumId(schedule.getStadiumId())
                .title(schedule.getTitle())
                .description(schedule.getDescription())
                .startDateTime(schedule.getStartDateTime())
                .endDateTime(schedule.getEndDateTime())
                .location(schedule.getLocation())
                .type(schedule.getType())
                .color(schedule.getColor())
                .url(schedule.getUrl())
                .isActive(schedule.getIsActive())
                .opponent(schedule.getOpponent() != null ? schedule.getOpponent() : schedule.getTitle())
                .season(schedule.resolveSeason())
                .isHome(schedule.resolveIsHome())
                .specialGame(schedule.getSpecialGame())
                .createdAt(schedule.getCreatedAt())
                .updatedAt(schedule.getUpdatedAt())
                .build();
    }
}
