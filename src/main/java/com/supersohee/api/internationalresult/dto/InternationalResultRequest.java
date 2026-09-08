package com.supersohee.api.internationalresult.dto;

import com.supersohee.api.internationalresult.domain.InternationalResultCategory;
import com.supersohee.api.internationalresult.domain.InternationalResultStatus;
import com.supersohee.api.internationalresult.domain.ParticipationStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;

public record InternationalResultRequest(
        @NotBlank @Size(max = 120)
        @Pattern(regexp = "[a-z0-9]+(?:-[a-z0-9]+)*", message = "must be a lowercase slug") String competitionKey,
        @NotBlank @Size(max = 200) String competitionName,
        @NotBlank @Size(max = 120) String editionLabel,
        @NotNull InternationalResultCategory category,
        @NotNull InternationalResultStatus status,
        @NotNull ParticipationStatus participationStatus,
        @NotNull LocalDate startDate,
        @NotNull LocalDate endDate,
        @NotBlank @Size(max = 200) String location,
        @NotBlank @Size(max = 120) String teamName,
        @Size(max = 200) String teamResult,
        @Size(max = 120) String teamRecord,
        @PositiveOrZero Integer gamesPlayed,
        @Size(max = 20) String minutesPerGame,
        @PositiveOrZero Double pointsPerGame,
        @PositiveOrZero Double reboundsPerGame,
        @PositiveOrZero Double assistsPerGame,
        @PositiveOrZero Double stealsPerGame,
        @PositiveOrZero @DecimalMax("100.0") Double fieldGoalPercent,
        @PositiveOrZero @DecimalMax("100.0") Double threePointPercent,
        @PositiveOrZero @DecimalMax("100.0") Double freeThrowPercent,
        @Size(max = 1000) String highlight,
        LocalDate statsUpdatedThrough,
        @NotEmpty @Size(max = 10) List<@Valid InternationalResultSourceRequest> sources,
        @NotNull Boolean published,
        @NotNull @PositiveOrZero Integer displayOrder) {
}
