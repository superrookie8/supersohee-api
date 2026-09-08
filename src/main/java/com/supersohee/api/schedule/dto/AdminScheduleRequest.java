package com.supersohee.api.schedule.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

public record AdminScheduleRequest(
        @Size(max = 100) String season,
        @NotBlank @Pattern(regexp = "\\d{4}-\\d{2}-\\d{2}") String date,
        @NotBlank @Pattern(regexp = "\\d{2}:\\d{2}") String time,
        @NotBlank @Size(max = 100) String opponent,
        Boolean isHome,
        String extraHome,
        Boolean specialGame,
        Boolean isActive,
        @Size(max = 100) String competitionKey,
        @Size(max = 100) String competitionName,
        @Size(max = 100) String competitionKind,
        @Size(max = 100) String editionLabel,
        @Size(max = 100) String teamType,
        @Size(max = 100) String ourTeamName,
        @Size(max = 100) String venueType,
        @Size(max = 200) String venueName,
        @Size(max = 100) String stage,
        @Size(max = 100) String stadiumId) {
    public AdminScheduleRequest(String season, String date, String time, String opponent,
            Boolean isHome, String extraHome, Boolean specialGame, Boolean isActive) {
        this(season, date, time, opponent, isHome, extraHome, specialGame, isActive,
                null, null, null, null, null, null, null, null, null, null);
    }
    public LocalDateTime startDateTime() {
        return LocalDateTime.of(LocalDate.parse(date), LocalTime.parse(time));
    }
}
