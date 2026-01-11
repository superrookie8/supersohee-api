package com.supersohee.api.player.controller;

import com.supersohee.api.player.domain.Player;
import com.supersohee.api.player.service.PlayerService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/player")
@RequiredArgsConstructor
public class PlayerController {

    private final PlayerService playerService;

    // 이소희 선수 프로필 조회 (공개, playerId 불필요)
    @GetMapping
    public ResponseEntity<Player> getSohee() {
        return playerService.findSohee()
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
