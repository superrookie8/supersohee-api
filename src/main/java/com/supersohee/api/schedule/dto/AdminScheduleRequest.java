package com.supersohee.api.schedule.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

public record AdminScheduleRequest(
        @NotBlank String season,
        @NotBlank @Pattern(regexp = "\\d{4}-\\d{2}-\\d{2}") String date,
        @NotBlank @Pattern(regexp = "\\d{2}:\\d{2}") String time,
        @NotBlank String opponent,
        @NotNull Boolean isHome,
        String extraHome,
        Boolean specialGame,
        Boolean isActive) {

    public LocalDateTime startDateTime() {
        return LocalDateTime.of(LocalDate.parse(date), LocalTime.parse(time));
    }
}
