package com.supersohee.api.playerstat.config;

import com.supersohee.api.playerstat.domain.PlayerStat;
import com.supersohee.api.playerstat.repository.PlayerStatRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PlayerStatDataInitializer {

        private final PlayerStatRepository playerStatRepository;

        @PostConstruct
        public void init() {
                // 이미 데이터가 있으면 스킵
                if (playerStatRepository.count() > 0) {
                        return;
                }

                // 2025-2026 시즌
                PlayerStat stat2026 = PlayerStat.builder()
                                .season("2025-2026")
                                .team("BNK 썸")
                                // 평균 기록
                                .gamesPlayed(13)
                                .minutesPerGame("33:52")
                                .twoPointPercent(38.5)
                                .threePointPercent(32.1)
                                .freeThrowPercent(80.8)
                                .offensiveRebounds(0.8)
                                .defensiveRebounds(3.7)
                                .totalRebounds(4.5)
                                .ppg(11.9)
                                .apg(1.7)
                                .spg(0.5)
                                .bpg(0.1)
                                .turnovers(1.2)
                                .fouls(2.5)
                                // 합계 기록
                                .totalMinutes("440:28")
                                .twoPointMade(40)
                                .twoPointAttempted(104)
                                .threePointMade(18)
                                .threePointAttempted(56)
                                .freeThrowMade(21)
                                .freeThrowAttempted(26)
                                .totalOffensiveRebounds(10)
                                .totalDefensiveRebounds(48)
                                .totalTotalRebounds(58)
                                .totalAssists(22)
                                .totalSteals(6)
                                .totalBlocks(1)
                                .totalTurnovers(16)
                                .totalFouls(33)
                                .totalPoints(155)
                                .build();

                // 2024-2025 시즌
                PlayerStat stat2025 = PlayerStat.builder()
                                .season("2024-2025")
                                .team("BNK 썸")
                                // 평균 기록
                                .gamesPlayed(18)
                                .minutesPerGame("31:46")
                                .twoPointPercent(36.3)
                                .threePointPercent(38.6)
                                .freeThrowPercent(89.2)
                                .offensiveRebounds(0.7)
                                .defensiveRebounds(2.8)
                                .totalRebounds(3.5)
                                .ppg(12.2)
                                .apg(1.5)
                                .spg(1.0)
                                .bpg(0.1)
                                .turnovers(1.3)
                                .fouls(2.8)
                                // 합계 기록
                                .totalMinutes("571:59")
                                .twoPointMade(45)
                                .twoPointAttempted(124)
                                .threePointMade(32)
                                .threePointAttempted(83)
                                .freeThrowMade(33)
                                .freeThrowAttempted(37)
                                .totalOffensiveRebounds(13)
                                .totalDefensiveRebounds(50)
                                .totalTotalRebounds(63)
                                .totalAssists(27)
                                .totalSteals(18)
                                .totalBlocks(1)
                                .totalTurnovers(23)
                                .totalFouls(51)
                                .totalPoints(219)
                                .build();

                // 2023-2024 시즌
                PlayerStat stat2024 = PlayerStat.builder()
                                .season("2023-2024")
                                .team("BNK 썸")
                                // 평균 기록
                                .gamesPlayed(29)
                                .minutesPerGame("35:00")
                                .twoPointPercent(43.2)
                                .threePointPercent(27.4)
                                .freeThrowPercent(87.0)
                                .offensiveRebounds(0.8)
                                .defensiveRebounds(4.2)
                                .totalRebounds(4.9)
                                .ppg(14.0)
                                .apg(2.6)
                                .spg(1.2)
                                .bpg(0.0)
                                .turnovers(1.8)
                                .fouls(2.8)
                                // 합계 기록
                                .totalMinutes("1015:26")
                                .twoPointMade(98)
                                .twoPointAttempted(227)
                                .threePointMade(48)
                                .threePointAttempted(175)
                                .freeThrowMade(67)
                                .freeThrowAttempted(77)
                                .totalOffensiveRebounds(22)
                                .totalDefensiveRebounds(121)
                                .totalTotalRebounds(143)
                                .totalAssists(76)
                                .totalSteals(35)
                                .totalBlocks(0)
                                .totalTurnovers(53)
                                .totalFouls(80)
                                .totalPoints(407)
                                .build();

                // 2022-2023 시즌
                PlayerStat stat2023 = PlayerStat.builder()
                                .season("2022-2023")
                                .team("BNK 썸")
                                // 평균 기록
                                .gamesPlayed(30)
                                .minutesPerGame("34:29")
                                .twoPointPercent(41.7)
                                .threePointPercent(37.6)
                                .freeThrowPercent(83.3)
                                .offensiveRebounds(0.7)
                                .defensiveRebounds(3.7)
                                .totalRebounds(4.4)
                                .ppg(16.9)
                                .apg(2.4)
                                .spg(1.4)
                                .bpg(0.0)
                                .turnovers(1.6)
                                .fouls(2.8)
                                // 합계 기록
                                .totalMinutes("1034:33")
                                .twoPointMade(95)
                                .twoPointAttempted(228)
                                .threePointMade(77)
                                .threePointAttempted(205)
                                .freeThrowMade(85)
                                .freeThrowAttempted(102)
                                .totalOffensiveRebounds(20)
                                .totalDefensiveRebounds(111)
                                .totalTotalRebounds(131)
                                .totalAssists(73)
                                .totalSteals(43)
                                .totalBlocks(1)
                                .totalTurnovers(49)
                                .totalFouls(85)
                                .totalPoints(506)
                                .build();

                // 2021-2022 시즌
                PlayerStat stat2022 = PlayerStat.builder()
                                .season("2021-2022")
                                .team("BNK 썸")
                                // 평균 기록
                                .gamesPlayed(30)
                                .minutesPerGame("30:50")
                                .twoPointPercent(37.5)
                                .threePointPercent(39.9)
                                .freeThrowPercent(76.3)
                                .offensiveRebounds(1.6)
                                .defensiveRebounds(2.5)
                                .totalRebounds(4.1)
                                .ppg(14.4)
                                .apg(1.7)
                                .spg(1.0)
                                .bpg(0.1)
                                .turnovers(1.5)
                                .fouls(3.0)
                                // 합계 기록
                                .totalMinutes("925:25")
                                .twoPointMade(72)
                                .twoPointAttempted(192)
                                .threePointMade(77)
                                .threePointAttempted(193)
                                .freeThrowMade(58)
                                .freeThrowAttempted(76)
                                .totalOffensiveRebounds(49)
                                .totalDefensiveRebounds(74)
                                .totalTotalRebounds(123)
                                .totalAssists(50)
                                .totalSteals(31)
                                .totalBlocks(2)
                                .totalTurnovers(45)
                                .totalFouls(90)
                                .totalPoints(433)
                                .build();

                // 2020-2021 시즌
                PlayerStat stat2021 = PlayerStat.builder()
                                .season("2020-2021")
                                .team("BNK 썸")
                                // 평균 기록
                                .gamesPlayed(30)
                                .minutesPerGame("31:25")
                                .twoPointPercent(43.4)
                                .threePointPercent(33.9)
                                .freeThrowPercent(76.4)
                                .offensiveRebounds(1.6)
                                .defensiveRebounds(3.1)
                                .totalRebounds(4.6)
                                .ppg(11.0)
                                .apg(2.2)
                                .spg(1.2)
                                .bpg(0.0)
                                .turnovers(1.0)
                                .fouls(2.8)
                                // 합계 기록
                                .totalMinutes("942:33")
                                .twoPointMade(82)
                                .twoPointAttempted(189)
                                .threePointMade(37)
                                .threePointAttempted(109)
                                .freeThrowMade(55)
                                .freeThrowAttempted(72)
                                .totalOffensiveRebounds(47)
                                .totalDefensiveRebounds(92)
                                .totalTotalRebounds(139)
                                .totalAssists(67)
                                .totalSteals(37)
                                .totalBlocks(1)
                                .totalTurnovers(31)
                                .totalFouls(85)
                                .totalPoints(330)
                                .build();

                // 2019-2020 시즌
                PlayerStat stat2020 = PlayerStat.builder()
                                .season("2019-2020")
                                .team("BNK 썸")
                                // 평균 기록
                                .gamesPlayed(9)
                                .minutesPerGame("20:34")
                                .twoPointPercent(26.5)
                                .threePointPercent(25.0)
                                .freeThrowPercent(60.0)
                                .offensiveRebounds(1.0)
                                .defensiveRebounds(2.4)
                                .totalRebounds(3.4)
                                .ppg(3.7)
                                .apg(1.3)
                                .spg(1.3)
                                .bpg(0.0)
                                .turnovers(0.9)
                                .fouls(2.3)
                                // 합계 기록
                                .totalMinutes("185:10")
                                .twoPointMade(9)
                                .twoPointAttempted(34)
                                .threePointMade(3)
                                .threePointAttempted(12)
                                .freeThrowMade(6)
                                .freeThrowAttempted(10)
                                .totalOffensiveRebounds(9)
                                .totalDefensiveRebounds(22)
                                .totalTotalRebounds(31)
                                .totalAssists(12)
                                .totalSteals(12)
                                .totalBlocks(0)
                                .totalTurnovers(8)
                                .totalFouls(21)
                                .totalPoints(33)
                                .build();

                // 2018-2019 시즌
                PlayerStat stat2019 = PlayerStat.builder()
                                .season("2018-2019")
                                .team("OK저축은행")
                                // 평균 기록
                                .gamesPlayed(15)
                                .minutesPerGame("17:35")
                                .twoPointPercent(47.1)
                                .threePointPercent(30.3)
                                .freeThrowPercent(80.0)
                                .offensiveRebounds(0.8)
                                .defensiveRebounds(1.2)
                                .totalRebounds(2.0)
                                .ppg(7.3)
                                .apg(0.8)
                                .spg(0.8)
                                .bpg(0.1)
                                .turnovers(1.3)
                                .fouls(1.4)
                                // 합계 기록
                                .totalMinutes("263:57")
                                .twoPointMade(24)
                                .twoPointAttempted(51)
                                .threePointMade(10)
                                .threePointAttempted(33)
                                .freeThrowMade(32)
                                .freeThrowAttempted(40)
                                .totalOffensiveRebounds(12)
                                .totalDefensiveRebounds(18)
                                .totalTotalRebounds(30)
                                .totalAssists(12)
                                .totalSteals(12)
                                .totalBlocks(1)
                                .totalTurnovers(20)
                                .totalFouls(21)
                                .totalPoints(110)
                                .build();

                playerStatRepository.save(stat2026);
                playerStatRepository.save(stat2025);
                playerStatRepository.save(stat2024);
                playerStatRepository.save(stat2023);
                playerStatRepository.save(stat2022);
                playerStatRepository.save(stat2021);
                playerStatRepository.save(stat2020);
                playerStatRepository.save(stat2019);

                System.out.println("이소희 선수 스탯 데이터가 생성되었습니다. (총 8개 시즌)");
        }
}