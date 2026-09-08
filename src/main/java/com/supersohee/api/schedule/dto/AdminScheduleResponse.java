package com.supersohee.api.schedule.dto;

import com.supersohee.api.schedule.domain.Schedule;

import java.time.format.DateTimeFormatter;

public record AdminScheduleResponse(
        String id,
        String season,
        String date,
        String time,
        String opponent,
        Boolean isHome,
        String extraHome,
        Boolean specialGame,
        Boolean isActive,
        String competitionKey,
        String competitionName,
        String competitionKind,
        String editionLabel,
        String teamType,
        String ourTeamName,
        String venueType,
        String venueName,
        String stage,
        String stadiumId) {

    public static AdminScheduleResponse from(Schedule schedule) {
        return new AdminScheduleResponse(
                schedule.getId(),
                schedule.resolveSeason(),
                schedule.getStartDateTime() != null
                        ? schedule.getStartDateTime().toLocalDate().toString()
                        : null,
                schedule.getStartDateTime() != null
                        ? schedule.getStartDateTime().toLocalTime().format(DateTimeFormatter.ofPattern("HH:mm"))
                        : null,
                schedule.getOpponent() != null ? schedule.getOpponent() : schedule.getTitle(),
                schedule.resolveIsHome(),
                schedule.getExtraHome(),
                schedule.resolveSpecialGame(),
                schedule.getIsActive(),
                schedule.resolveCompetitionKey(),
                schedule.resolveCompetitionName(),
                schedule.resolveCompetitionKind(),
                schedule.resolveEditionLabel(),
                schedule.resolveTeamType(),
                schedule.resolveOurTeamName(),
                schedule.resolveVenueType(),
                schedule.resolveVenueName(),
                schedule.getStage(),
                schedule.getStadiumId());
    }
}
