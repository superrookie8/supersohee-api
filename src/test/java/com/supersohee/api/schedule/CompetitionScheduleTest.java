package com.supersohee.api.schedule;

import com.supersohee.api.admin.error.AdminApiException;
import com.supersohee.api.game.repository.GameRepository;
import com.supersohee.api.schedule.domain.Schedule;
import com.supersohee.api.schedule.dto.*;
import com.supersohee.api.schedule.repository.ScheduleRepository;
import com.supersohee.api.schedule.service.ScheduleService;
import com.supersohee.api.stadium.service.StadiumService;
import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

class CompetitionScheduleTest {
    final ScheduleRepository repository = mock(ScheduleRepository.class);
    final StadiumService stadium = mock(StadiumService.class);
    final ScheduleService service = new ScheduleService(repository, stadium, mock(GameRepository.class));
    CompetitionScheduleTest() { when(repository.save(any())).thenAnswer(i -> i.getArgument(0)); }

    AdminScheduleRequest request(String key, String name, String kind, String edition, String team, String venue, String venueName) {
        return new AdminScheduleRequest(null, "2027-01-02", "19:00", "자유 상대팀", null, null, false, true,
                key, name, kind, edition, team, team.equals("national") ? "대한민국" : "BNK 썸", venue, venueName, "결선", null);
    }

    @Test void arbitraryCompetitionsAndPostponedEditionRoundTripWithoutDateInference() {
        for (String key : List.of("park-shinja", "world-cup", "asian-games", "olympics", "직접입력-친선")) {
            String team = key.equals("park-shinja") ? "club" : "national";
            Schedule s = service.createAdminSchedule(request(key, key, "tournament", "2026 연기 대회", team, "neutral", null));
            assertThat(s.resolveSeason()).isNull();
            assertThat(s.resolveEditionLabel()).isEqualTo("2026 연기 대회");
            assertThat(s.getLocation()).isNull();
            assertThat(s.getType()).isEqualTo("game");
            assertThat(s.getIsHome()).isFalse();
            assertThat(s.getSpecialGame()).isFalse();
            assertThat(AdminScheduleResponse.from(s).competitionKey()).isEqualTo(key);
            assertThat(ScheduleResponse.from(s).getCompetitionName()).isEqualTo(key);
            assertThat(ScheduleDetailsResponse.from(s, null, "same-id").getEditionLabel()).isEqualTo("2026 연기 대회");
            assertThat(service.filterSchedules(List.of(s), null, key, "2026 연기 대회", team)).containsExactly(s);
        }
        var nationalLeague = service.createAdminSchedule(request("custom", "직접 대회", "league", "2026-2028", "national", "home", null));
        assertThat(nationalLeague.getCompetitionKind()).isEqualTo("league");
        assertThat(nationalLeague.getTeamType()).isEqualTo("national");
        assertThat(nationalLeague.resolveSeason()).isNull();
    }

    @Test void legacyDefaultsDoNotMisclassifySpecialGamesOrEvents() {
        Schedule normal = Schedule.builder().id("old").type("game").location("Home").startDateTime(LocalDateTime.of(2026,1,2,19,0)).build();
        Schedule special = normal.toBuilder().type("specialGame").build();
        Schedule event = normal.toBuilder().type("event").build();
        assertThat(normal.resolveCompetitionKey()).isEqualTo("wkbl");
        assertThat(normal.resolveEditionLabel()).isEqualTo("2025-2026");
        assertThat(special.resolveCompetitionKey()).isEqualTo("legacy-special");
        assertThat(special.resolveOurTeamName()).isNull();
        assertThat(special.resolveTeamType()).isNull();
        assertThat(event.resolveCompetitionKey()).isNull();
        assertThat(service.filterSchedules(List.of(normal,special,event), "2025-2026", "wkbl", "2025-2026", "club")).containsExactly(normal);
        assertThat(service.filterSchedules(List.of(normal,special), null, "wkbl", null, "national")).isEmpty();
    }

    @Test void oldClientUpdateAndDeletePreserveStoredMetadataAndIds() {
        Schedule original = service.createAdminSchedule(request("olympics", "올림픽", "tournament", "2026", "national", "neutral", "중립 체육관"))
                .toBuilder().id("preserved-id").gameId("existing-link").build();
        when(repository.findById("preserved-id")).thenReturn(Optional.of(original));
        Schedule updated = service.updateAdminSchedule("preserved-id", new AdminScheduleRequest(
                "2026-2027", "2028-01-02", "18:00", "다른 국가", true, null, null, true));
        assertThat(updated.getId()).isEqualTo("preserved-id");
        assertThat(updated.getGameId()).isEqualTo("existing-link");
        assertThat(updated.getCompetitionKey()).isEqualTo("olympics");
        assertThat(updated.getEditionLabel()).isEqualTo("2026");
        assertThat(updated.getVenueName()).isEqualTo("중립 체육관");
        assertThat(updated.getVenueType()).isEqualTo("neutral");
        assertThat(updated.getIsHome()).isFalse();
        service.deleteSchedule("preserved-id");
        verify(repository).save(argThat(s -> "preserved-id".equals(s.getId()) && Boolean.FALSE.equals(s.getIsActive()) && "olympics".equals(s.getCompetitionKey())));
    }

    @Test void completeMetadataUpdateClearsOptionalFieldsAndPreservesLegacySeason() {
        Schedule old = service.createAdminSchedule(request("cup", "컵", "tournament", "2026", "club", "neutral", "기존 장소"))
                .toBuilder().id("same").season("2025-2026").stadiumId("old-stadium").stage("준결승").build();
        when(repository.findById("same")).thenReturn(Optional.of(old));
        var r = new AdminScheduleRequest(null,"2027-01-02","20:00","상대",null,null,false,true,
                "cup","컵",null,"2026","club","BNK 썸","neutral","", "", "");
        Schedule result = service.updateAdminSchedule("same",r);
        assertThat(result.getSeason()).isEqualTo("2025-2026");
        assertThat(result.getVenueName()).isNull();
        assertThat(result.getStadiumId()).isNull();
        assertThat(result.getStage()).isNull();
        assertThat(result.getLocation()).isNull();
    }

    @Test void newMetadataUnknownVenueNeverInfersSajikAndKeepsDiaryLink() {
        Schedule s = service.createAdminSchedule(request("cup","컵","tournament","2026","club","home",null)).toBuilder().id("match").build();
        when(repository.findById("match")).thenReturn(Optional.of(s));
        var details = service.findDetailsById("match").orElseThrow();
        assertThat(details.getStadium()).isNull();
        assertThat(details.getGameId()).isEqualTo("match");
        verifyNoInteractions(stadium);
    }

    @Test void partialGroupContradictoryHomeInvalidDatesAndFiltersRejectBeforeSave() {
        var partial = new AdminScheduleRequest("2026-2027","2026-01-02","19:00","상대",true,null,false,true,
                "cup",null,null,null,null,null,null,null,null,null);
        assertThatThrownBy(() -> service.createAdminSchedule(partial)).isInstanceOf(AdminApiException.class);
        var conflict = new AdminScheduleRequest(null,"2026-01-02","19:00","상대",true,null,false,true,
                "cup","컵",null,"2026","club","BNK 썸","neutral",null,null,null);
        assertThatThrownBy(() -> service.createAdminSchedule(conflict)).isInstanceOf(AdminApiException.class);
        assertThatThrownBy(() -> service.createAdminSchedule(new AdminScheduleRequest("2026-2027","2026-02-30","19:00","상대",true,null,false,true))).isInstanceOf(AdminApiException.class);
        assertThatThrownBy(() -> service.filterSchedules(List.of(),null,null,null,"invalid")).isInstanceOf(AdminApiException.class);
        assertThatThrownBy(() -> service.filterSchedules(List.of(),null,"a".repeat(101),null,null)).isInstanceOf(AdminApiException.class);
        verify(repository,never()).save(any());
    }
}
