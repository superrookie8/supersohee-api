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
        Boolean isActive) {

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
                schedule.getIsActive());
    }
}
