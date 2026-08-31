package com.supersohee.api.playerstat.controller;

import com.supersohee.api.admin.error.AdminApiException;
import com.supersohee.api.playerstat.dto.PlayerStatRequest;
import com.supersohee.api.playerstat.dto.PlayerStatResponse;
import com.supersohee.api.playerstat.service.PlayerStatService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/playerstat")
@RequiredArgsConstructor
public class AdminPlayerStatController {

    private final PlayerStatService playerStatService;

    @GetMapping
    public List<PlayerStatResponse> getAllPlayerStats() {
        return playerStatService.findAllSoheeStats().stream()
                .map(PlayerStatResponse::from)
                .toList();
    }

    @GetMapping("/{id}")
    public PlayerStatResponse getPlayerStat(@PathVariable String id) {
        return playerStatService.findById(id)
                .map(PlayerStatResponse::from)
                .orElseThrow(() -> AdminApiException.notFound("Player stat"));
    }

    @PostMapping
    public PlayerStatResponse upsertPlayerStat(@Valid @RequestBody PlayerStatRequest request) {
        return PlayerStatResponse.from(playerStatService.upsertPlayerStat(request));
    }

    @PutMapping("/{id}")
    public PlayerStatResponse updatePlayerStat(
            @PathVariable String id,
            @Valid @RequestBody PlayerStatRequest request) {
        return PlayerStatResponse.from(playerStatService.updatePlayerStat(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePlayerStat(@PathVariable String id) {
        playerStatService.deletePlayerStat(id);
        return ResponseEntity.noContent().build();
    }
}
