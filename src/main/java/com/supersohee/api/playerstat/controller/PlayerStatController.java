package com.supersohee.api.playerstat.controller;

import com.supersohee.api.playerstat.domain.PlayerStat;
import com.supersohee.api.playerstat.service.PlayerStatService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/playerstat")
@RequiredArgsConstructor
public class PlayerStatController {

    private final PlayerStatService playerStatService;

    // 이소희 선수 특정 시즌 스탯 조회 (공개, playerId 불필요)
    @GetMapping
    public ResponseEntity<PlayerStat> getSoheeStatBySeason(
            @RequestParam String season) {
        return playerStatService.findSoheeStatBySeason(season)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // 이소희 선수 모든 시즌 스탯 조회 (공개, playerId 불필요)
    @GetMapping("/all")
    public ResponseEntity<List<PlayerStat>> getAllSoheeStats() {
        List<PlayerStat> stats = playerStatService.findAllSoheeStats();
        return ResponseEntity.ok(stats);
    }
}
