package com.supersohee.api.schedule.service;

import com.supersohee.api.admin.error.AdminApiException;
import com.supersohee.api.schedule.domain.Schedule;
import com.supersohee.api.schedule.dto.AdminScheduleRequest;
import java.util.Map;
import java.util.Set;

/** Validates metadata before persistence; old clients leave stored competition data intact. */
final class ScheduleCompetitionPolicy {
    private ScheduleCompetitionPolicy() {}

    static Schedule apply(AdminScheduleRequest r, Schedule previous) {
        if (r == null) throw invalid("request", "Schedule is required.");
        text(r.opponent(), "opponent", 100, true);
        text(r.season(), "season", 100, false);
        try { r.startDateTime(); } catch (RuntimeException e) { throw invalid("date", "A valid calendar date and HH:mm time are required."); }
        boolean supplied = java.util.stream.Stream.of(r.competitionKey(), r.competitionName(), r.competitionKind(),
                r.editionLabel(), r.teamType(), r.ourTeamName(), r.venueType(), r.venueName(), r.stage(), r.stadiumId())
                .anyMatch(java.util.Objects::nonNull);
        boolean preserve = !supplied && previous != null && previous.hasCompetitionMetadata();
        if (supplied) {
            text(r.competitionKey(), "competitionKey", 100, true);
            text(r.competitionName(), "competitionName", 100, true);
            text(r.editionLabel(), "editionLabel", 100, true);
            text(r.ourTeamName(), "ourTeamName", 100, true);
            choice(r.teamType(), "teamType", Set.of("club", "national"), true);
            choice(r.venueType(), "venueType", Set.of("home", "away", "neutral"), true);
            choice(r.competitionKind(), "competitionKind", Set.of("league", "tournament", "friendly", "other"), false);
            text(r.venueName(), "venueName", 200, false);
            text(r.stage(), "stage", 100, false);
            text(r.stadiumId(), "stadiumId", 100, false);
            if (r.isHome() != null && r.isHome() != "home".equals(r.venueType()))
                throw invalid("isHome", "isHome must agree with venueType.");
        } else if (!preserve) {
            text(r.season(), "season", 100, true);
            if (r.isHome() == null) throw invalid("isHome", "isHome is required for a legacy schedule.");
        }
        Schedule.ScheduleBuilder b = previous == null ? Schedule.builder() : previous.toBuilder();
        boolean metadata = supplied || preserve;
        boolean home = supplied ? "home".equals(r.venueType()) : preserve ? "home".equals(previous.getVenueType()) : Boolean.TRUE.equals(r.isHome());
        boolean special = r.specialGame() != null ? r.specialGame() : previous != null && previous.resolveSpecialGame();
        b.title(r.opponent().trim()).opponent(r.opponent().trim())
                .startDateTime(r.startDateTime()).endDateTime(r.startDateTime().plusHours(2))
                .isHome(home).color(home ? "#EF4444" : "#3B82F6")
                .specialGame(special).type(special ? "specialGame" : "game")
                .isActive(r.isActive() != null ? r.isActive() : previous == null || !Boolean.FALSE.equals(previous.getIsActive()));
        if (r.season() != null || previous == null) b.season(clean(r.season()));
        if (r.extraHome() != null || !metadata) b.extraHome(r.extraHome());
        if (supplied) {
            // A complete metadata group replaces optional values: omitted/empty venue,
            // stage and stadium reference clear them. Metadata-free updates preserve them.
            b.competitionKey(clean(r.competitionKey())).competitionName(clean(r.competitionName()))
                    .competitionKind(clean(r.competitionKind())).editionLabel(clean(r.editionLabel()))
                    .teamType(r.teamType()).ourTeamName(clean(r.ourTeamName()))
                    .venueType(r.venueType()).venueName(clean(r.venueName())).stage(clean(r.stage()))
                    .stadiumId(clean(r.stadiumId())).location(clean(r.venueName()));
        } else if (!preserve) {
            b.location(home ? "Home" : r.opponent().trim());
        }
        Schedule result = b.build();
        if (previous != null) result.setCreatedAt(previous.getCreatedAt());
        return result;
    }

    static void validateFilters(String season, String key, String edition, String type) {
        text(season, "season", 100, false); text(key, "competitionKey", 100, false);
        text(edition, "editionLabel", 100, false);
        choice(clean(type), "teamType", Set.of("club", "national"), false);
    }
    static boolean matches(Schedule s, String season, String key, String edition, String type) {
        return matches(season, s.resolveSeason()) && matches(key, s.resolveCompetitionKey())
                && matches(edition, s.resolveEditionLabel()) && matches(type, s.resolveTeamType());
    }
    private static boolean matches(String filter, String value) { return clean(filter) == null || clean(filter).equals(value); }
    private static void choice(String v, String field, Set<String> values, boolean required) {
        if (v == null && !required) return;
        if (!values.contains(v == null ? "" : v)) throw invalid(field, "Unsupported " + field + ".");
    }
    private static void text(String value, String field, int max, boolean required) {
        if (value == null) { if (required) throw invalid(field, field + " is required."); return; }
        if (value.length() > max || value.chars().anyMatch(Character::isISOControl) || required && value.isBlank())
            throw invalid(field, "Invalid " + field + ".");
    }
    private static String clean(String value) { return value == null || value.isBlank() ? null : value.trim(); }
    private static AdminApiException invalid(String field, String message) { return AdminApiException.unprocessable(message, Map.of(field, message)); }
}
