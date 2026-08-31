package com.supersohee.api.schedule;

import com.supersohee.api.schedule.domain.Schedule;
import com.supersohee.api.schedule.service.ScheduleService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SchedulePublicContractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ScheduleService scheduleService;

    @Test
    void scheduleListAddsGameMetadataAndPreservesLegacyFallbacks() throws Exception {
        Schedule current = Schedule.builder()
                .id("schedule-current")
                .title("정규 경기")
                .startDateTime(LocalDateTime.of(2026, 1, 2, 19, 0))
                .location("Home")
                .type("game")
                .opponent("KB스타즈")
                .isHome(false)
                .specialGame(true)
                .isActive(true)
                .build();
        Schedule legacy = Schedule.builder()
                .id("schedule-legacy")
                .title("우리은행")
                .startDateTime(LocalDateTime.of(2026, 1, 5, 19, 0))
                .location("Home")
                .type("game")
                .isActive(true)
                .build();
        when(scheduleService.findActiveSchedules(null, null)).thenReturn(List.of(current, legacy));

        mockMvc.perform(get("/api/schedules"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("schedule-current"))
                .andExpect(jsonPath("$[0].title").value("정규 경기"))
                .andExpect(jsonPath("$[0].location").value("Home"))
                .andExpect(jsonPath("$[0].opponent").value("KB스타즈"))
                .andExpect(jsonPath("$[0].isHome").value(false))
                .andExpect(jsonPath("$[0].specialGame").value(true))
                .andExpect(jsonPath("$[1].opponent").value("우리은행"))
                .andExpect(jsonPath("$[1].isHome").value(true))
                .andExpect(jsonPath("$[1].specialGame").value(nullValue()));

        verify(scheduleService).findActiveSchedules(null, null);
    }
}
