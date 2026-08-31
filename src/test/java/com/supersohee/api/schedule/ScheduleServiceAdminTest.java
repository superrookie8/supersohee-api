package com.supersohee.api.schedule;

import com.supersohee.api.game.repository.GameRepository;
import com.supersohee.api.schedule.domain.Schedule;
import com.supersohee.api.schedule.dto.AdminScheduleRequest;
import com.supersohee.api.schedule.repository.ScheduleRepository;
import com.supersohee.api.schedule.service.ScheduleService;
import com.supersohee.api.stadium.service.StadiumService;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ScheduleServiceAdminTest {

    private final ScheduleRepository scheduleRepository = mock(ScheduleRepository.class);
    private final StadiumService stadiumService = mock(StadiumService.class);
    private final GameRepository gameRepository = mock(GameRepository.class);
    private final ScheduleService service = new ScheduleService(
            scheduleRepository, stadiumService, gameRepository);

    @Test
    void adminListsResolveLegacySeasonsExcludeInactiveAndUseContractOrdering() {
        Schedule spring = schedule("spring", LocalDateTime.of(2026, 3, 30, 19, 0), null, true);
        Schedule autumn = schedule("autumn", LocalDateTime.of(2025, 10, 1, 19, 0), "2025-2026", null);
        Schedule nextSeason = schedule("next", LocalDateTime.of(2026, 10, 1, 19, 0), "2026-2027", true);
        Schedule deleted = schedule("deleted", LocalDateTime.of(2030, 10, 1, 19, 0), "2030-2031", false);
        when(scheduleRepository.findAllByOrderByStartDateTimeDesc())
                .thenReturn(List.of(deleted, nextSeason, spring, autumn));

        assertThat(service.findAdminSchedules("2025-2026"))
                .extracting(Schedule::getId)
                .containsExactly("autumn", "spring");
        assertThat(service.findAdminSeasons())
                .containsExactly("2026-2027", "2025-2026");
    }

    @Test
    void adminCreateAndUpdateKeepOpponentVenueAndFlagsDistinct() {
        when(scheduleRepository.save(any(Schedule.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Schedule created = service.createAdminSchedule(new AdminScheduleRequest(
                "2025-2026", "2026-01-02", "19:00", "KB스타즈",
                true, "사직", true, true));

        assertThat(created.getTitle()).isEqualTo("KB스타즈");
        assertThat(created.getOpponent()).isEqualTo("KB스타즈");
        assertThat(created.getLocation()).isEqualTo("Home");
        assertThat(created.getExtraHome()).isEqualTo("사직");
        assertThat(created.getIsHome()).isTrue();
        assertThat(created.getSpecialGame()).isTrue();
        assertThat(created.getType()).isEqualTo("specialGame");
        assertThat(created.getColor()).isEqualTo("#EF4444");

        Schedule existing = Schedule.builder()
                .id("schedule-1")
                .title("KB스타즈")
                .description("기존 설명")
                .startDateTime(LocalDateTime.of(2026, 1, 2, 19, 0))
                .location("Home")
                .type("specialGame")
                .color("#EF4444")
                .url("/schedule/1")
                .stadiumId("stadium-1")
                .gameId("game-1")
                .season("2025-2026")
                .opponent("KB스타즈")
                .isHome(true)
                .extraHome("사직")
                .specialGame(true)
                .isActive(true)
                .build();
        when(scheduleRepository.findById("schedule-1")).thenReturn(Optional.of(existing));

        Schedule updated = service.updateAdminSchedule("schedule-1", new AdminScheduleRequest(
                "2025-2026", "2026-01-03", "19:00", "우리은행",
                false, null, false, true));

        assertThat(updated.getTitle()).isEqualTo("우리은행");
        assertThat(updated.getOpponent()).isEqualTo("우리은행");
        assertThat(updated.getLocation()).isEqualTo("우리은행");
        assertThat(updated.getExtraHome()).isNull();
        assertThat(updated.getIsHome()).isFalse();
        assertThat(updated.getSpecialGame()).isFalse();
        assertThat(updated.getType()).isEqualTo("game");
        assertThat(updated.getColor()).isEqualTo("#3B82F6");
        assertThat(updated.getDescription()).isEqualTo("기존 설명");
        assertThat(updated.getStadiumId()).isEqualTo("stadium-1");
        assertThat(updated.getGameId()).isEqualTo("game-1");
    }

    private Schedule schedule(
            String id,
            LocalDateTime startDateTime,
            String season,
            Boolean isActive) {
        return Schedule.builder()
                .id(id)
                .title(id)
                .startDateTime(startDateTime)
                .season(season)
                .isActive(isActive)
                .build();
    }
}
