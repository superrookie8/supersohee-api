package com.supersohee.api.playerstat.service;

import com.supersohee.api.playerstat.domain.PlayerStat;
import com.supersohee.api.playerstat.dto.PlayerStatRequest;
import com.supersohee.api.playerstat.repository.PlayerStatRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PlayerStatService {

    private final PlayerStatRepository playerStatRepository;

    // 이소희 선수 특정 시즌 스탯 조회
    public Optional<PlayerStat> findSoheeStatBySeason(String season) {
        return playerStatRepository.findBySeason(season);
    }

    // 이소희 선수 모든 시즌 스탯 조회
    public List<PlayerStat> findAllSoheeStats() {
        return playerStatRepository.findAllByOrderBySeasonDesc();
    }

    // 특정 스탯 조회 (ID로)
    public Optional<PlayerStat> findById(String id) {
        return playerStatRepository.findById(id);
    }

    // 스탯 생성
    @Transactional
    public PlayerStat createPlayerStat(PlayerStatRequest request) {
        // 같은 시즌의 스탯이 이미 있는지 확인
        if (request.getSeason() != null) {
            Optional<PlayerStat> existing = playerStatRepository.findBySeason(request.getSeason());
            if (existing.isPresent()) {
                throw new RuntimeException("이미 해당 시즌의 스탯이 존재합니다: " + request.getSeason());
            }
        }

        PlayerStat playerStat = PlayerStat.builder()
                .season(request.getSeason())
                .team(request.getTeam())
                .gamesPlayed(request.getGamesPlayed())
                .minutesPerGame(request.getMinutesPerGame())
                .twoPointPercent(request.getTwoPointPercent())
                .threePointPercent(request.getThreePointPercent())
                .freeThrowPercent(request.getFreeThrowPercent())
                .offensiveRebounds(request.getOffensiveRebounds())
                .defensiveRebounds(request.getDefensiveRebounds())
                .totalRebounds(request.getTotalRebounds())
                .ppg(request.getPpg())
                .apg(request.getApg())
                .spg(request.getSpg())
                .bpg(request.getBpg())
                .turnovers(request.getTurnovers())
                .fouls(request.getFouls())
                .totalMinutes(request.getTotalMinutes())
                .twoPointMade(request.getTwoPointMade())
                .twoPointAttempted(request.getTwoPointAttempted())
                .threePointMade(request.getThreePointMade())
                .threePointAttempted(request.getThreePointAttempted())
                .freeThrowMade(request.getFreeThrowMade())
                .freeThrowAttempted(request.getFreeThrowAttempted())
                .totalOffensiveRebounds(request.getTotalOffensiveRebounds())
                .totalDefensiveRebounds(request.getTotalDefensiveRebounds())
                .totalTotalRebounds(request.getTotalTotalRebounds())
                .totalAssists(request.getTotalAssists())
                .totalSteals(request.getTotalSteals())
                .totalBlocks(request.getTotalBlocks())
                .totalTurnovers(request.getTotalTurnovers())
                .totalFouls(request.getTotalFouls())
                .totalPoints(request.getTotalPoints())
                .build();

        return playerStatRepository.save(playerStat);
    }

    // 스탯 수정 (부분 업데이트 지원)
    @Transactional
    public PlayerStat updatePlayerStat(String id, PlayerStatRequest request) {
        PlayerStat playerStat = playerStatRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("스탯을 찾을 수 없습니다"));

        // 부분 업데이트: request에 있는 필드만 업데이트
        PlayerStat.PlayerStatBuilder builder = playerStat.toBuilder();

        if (request.getSeason() != null) {
            // 시즌 변경 시 중복 체크
            if (!request.getSeason().equals(playerStat.getSeason())) {
                Optional<PlayerStat> existing = playerStatRepository.findBySeason(request.getSeason());
                if (existing.isPresent() && !existing.get().getId().equals(id)) {
                    throw new RuntimeException("이미 해당 시즌의 스탯이 존재합니다: " + request.getSeason());
                }
            }
            builder.season(request.getSeason());
        }
        if (request.getTeam() != null)
            builder.team(request.getTeam());
        if (request.getGamesPlayed() != null)
            builder.gamesPlayed(request.getGamesPlayed());
        if (request.getMinutesPerGame() != null)
            builder.minutesPerGame(request.getMinutesPerGame());
        if (request.getTwoPointPercent() != null)
            builder.twoPointPercent(request.getTwoPointPercent());
        if (request.getThreePointPercent() != null)
            builder.threePointPercent(request.getThreePointPercent());
        if (request.getFreeThrowPercent() != null)
            builder.freeThrowPercent(request.getFreeThrowPercent());
        if (request.getOffensiveRebounds() != null)
            builder.offensiveRebounds(request.getOffensiveRebounds());
        if (request.getDefensiveRebounds() != null)
            builder.defensiveRebounds(request.getDefensiveRebounds());
        if (request.getTotalRebounds() != null)
            builder.totalRebounds(request.getTotalRebounds());
        if (request.getPpg() != null)
            builder.ppg(request.getPpg());
        if (request.getApg() != null)
            builder.apg(request.getApg());
        if (request.getSpg() != null)
            builder.spg(request.getSpg());
        if (request.getBpg() != null)
            builder.bpg(request.getBpg());
        if (request.getTurnovers() != null)
            builder.turnovers(request.getTurnovers());
        if (request.getFouls() != null)
            builder.fouls(request.getFouls());
        if (request.getTotalMinutes() != null)
            builder.totalMinutes(request.getTotalMinutes());
        if (request.getTwoPointMade() != null)
            builder.twoPointMade(request.getTwoPointMade());
        if (request.getTwoPointAttempted() != null)
            builder.twoPointAttempted(request.getTwoPointAttempted());
        if (request.getThreePointMade() != null)
            builder.threePointMade(request.getThreePointMade());
        if (request.getThreePointAttempted() != null)
            builder.threePointAttempted(request.getThreePointAttempted());
        if (request.getFreeThrowMade() != null)
            builder.freeThrowMade(request.getFreeThrowMade());
        if (request.getFreeThrowAttempted() != null)
            builder.freeThrowAttempted(request.getFreeThrowAttempted());
        if (request.getTotalOffensiveRebounds() != null)
            builder.totalOffensiveRebounds(request.getTotalOffensiveRebounds());
        if (request.getTotalDefensiveRebounds() != null)
            builder.totalDefensiveRebounds(request.getTotalDefensiveRebounds());
        if (request.getTotalTotalRebounds() != null)
            builder.totalTotalRebounds(request.getTotalTotalRebounds());
        if (request.getTotalAssists() != null)
            builder.totalAssists(request.getTotalAssists());
        if (request.getTotalSteals() != null)
            builder.totalSteals(request.getTotalSteals());
        if (request.getTotalBlocks() != null)
            builder.totalBlocks(request.getTotalBlocks());
        if (request.getTotalTurnovers() != null)
            builder.totalTurnovers(request.getTotalTurnovers());
        if (request.getTotalFouls() != null)
            builder.totalFouls(request.getTotalFouls());
        if (request.getTotalPoints() != null)
            builder.totalPoints(request.getTotalPoints());

        return playerStatRepository.save(builder.build());
    }

    // 스탯 삭제
    @Transactional
    public void deletePlayerStat(String id) {
        PlayerStat playerStat = playerStatRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("스탯을 찾을 수 없습니다"));
        playerStatRepository.delete(playerStat);
    }
}
