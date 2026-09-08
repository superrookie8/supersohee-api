package com.supersohee.api.internationalresult.dto;

import com.supersohee.api.internationalresult.domain.InternationalResult;
import com.supersohee.api.internationalresult.domain.InternationalResultCategory;
import com.supersohee.api.internationalresult.domain.InternationalResultSource;
import com.supersohee.api.internationalresult.domain.InternationalResultSourceType;
import com.supersohee.api.internationalresult.domain.InternationalResultStatus;
import com.supersohee.api.internationalresult.domain.ParticipationStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record InternationalResultResponse(
        String id,
        String competitionKey,
        String competitionName,
        String editionLabel,
        InternationalResultCategory category,
        InternationalResultStatus status,
        ParticipationStatus participationStatus,
        LocalDate startDate,
        LocalDate endDate,
        String location,
        String teamName,
        String teamResult,
        String teamRecord,
        Integer gamesPlayed,
        String minutesPerGame,
        Double pointsPerGame,
        Double reboundsPerGame,
        Double assistsPerGame,
        Double stealsPerGame,
        Double fieldGoalPercent,
        Double threePointPercent,
        Double freeThrowPercent,
        String highlight,
        LocalDate statsUpdatedThrough,
        List<Source> sources,
        boolean published,
        int displayOrder,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {

    public static InternationalResultResponse from(InternationalResult result) {
        return new InternationalResultResponse(
                result.getId(), result.getCompetitionKey(), result.getCompetitionName(), result.getEditionLabel(),
                result.getCategory(), result.getStatus(), result.getParticipationStatus(), result.getStartDate(),
                result.getEndDate(), result.getLocation(), result.getTeamName(), result.getTeamResult(),
                result.getTeamRecord(), result.getGamesPlayed(), result.getMinutesPerGame(), result.getPointsPerGame(),
                result.getReboundsPerGame(), result.getAssistsPerGame(), result.getStealsPerGame(), result.getFieldGoalPercent(),
                result.getThreePointPercent(), result.getFreeThrowPercent(), result.getHighlight(),
                result.getStatsUpdatedThrough(),
                result.getSources() == null ? List.of() : result.getSources().stream().map(Source::from).toList(),
                result.isPublished(), result.getDisplayOrder(), result.getCreatedAt(), result.getUpdatedAt());
    }

    public record Source(String label, String url, InternationalResultSourceType type) {
        static Source from(InternationalResultSource source) {
            return new Source(source.getLabel(), source.getUrl(), source.getType());
        }
    }
}
