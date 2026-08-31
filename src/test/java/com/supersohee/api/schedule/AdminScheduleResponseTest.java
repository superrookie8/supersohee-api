package com.supersohee.api.schedule;

import com.supersohee.api.schedule.domain.Schedule;
import com.supersohee.api.schedule.dto.AdminScheduleResponse;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class AdminScheduleResponseTest {

    // season 계약 이전에 적재된 경기는 season/isHome 필드가 비어 있어
    // 어드민 화면에서 시즌 뱃지도, 목록도 나오지 않았다.
    @Test
    void legacyScheduleWithoutSeasonResolvesSeasonFromGameDate() {
        AdminScheduleResponse autumn = AdminScheduleResponse.from(Schedule.builder()
                .id("autumn")
                .title("신한은행")
                .startDateTime(LocalDateTime.of(2025, 11, 16, 14, 0))
                .location("Home")
                .type("game")
                .opponent("신한은행")
                .isActive(true)
                .build());

        assertThat(autumn.season()).isEqualTo("2025-2026");
        assertThat(autumn.isHome()).isTrue();
        assertThat(autumn.specialGame()).isFalse();

        AdminScheduleResponse spring = AdminScheduleResponse.from(Schedule.builder()
                .id("spring")
                .title("KB스타즈")
                .startDateTime(LocalDateTime.of(2026, 3, 30, 19, 0))
                .location("KB스타즈")
                .type("game")
                .opponent("KB스타즈")
                .isActive(true)
                .build());

        assertThat(spring.season()).isEqualTo("2025-2026");
        assertThat(spring.isHome()).isFalse();
    }

    // 올스타전처럼 type만 specialGame인 과거 문서가 어드민 수정 시 일반 경기로 강등되면 안 된다.
    @Test
    void legacySpecialGameTypeResolvesToSpecialGameFlag() {
        AdminScheduleResponse response = AdminScheduleResponse.from(Schedule.builder()
                .id("all-star")
                .title("포니블")
                .description("올스타전 팀유니블 vs 팀포니블")
                .startDateTime(LocalDateTime.of(2026, 1, 4, 14, 0))
                .location("Home")
                .type("specialGame")
                .opponent("포니블")
                .isActive(true)
                .build());

        assertThat(response.specialGame()).isTrue();
        assertThat(response.isHome()).isTrue();
    }

    // 명시적으로 저장된 값은 유추보다 우선한다.
    @Test
    void storedFieldsWinOverDerivedValues() {
        AdminScheduleResponse response = AdminScheduleResponse.from(Schedule.builder()
                .id("stored")
                .startDateTime(LocalDateTime.of(2026, 1, 4, 14, 0))
                .location("Home")
                .type("game")
                .season("2030-2031")
                .opponent("하나은행")
                .isHome(false)
                .specialGame(false)
                .isActive(true)
                .build());

        assertThat(response.season()).isEqualTo("2030-2031");
        assertThat(response.isHome()).isFalse();
        assertThat(response.specialGame()).isFalse();
    }

    @Test
    void legacyTitleFallbackKeepsOpponentAndExtraHomeSeparate() {
        AdminScheduleResponse response = AdminScheduleResponse.from(Schedule.builder()
                .id("legacy-home")
                .title("우리은행")
                .startDateTime(LocalDateTime.of(2026, 2, 1, 14, 0))
                .location("Home")
                .extraHome("사직")
                .type("game")
                .isActive(true)
                .build());

        assertThat(response.opponent()).isEqualTo("우리은행");
        assertThat(response.extraHome()).isEqualTo("사직");
        assertThat(response.isHome()).isTrue();
    }
}
