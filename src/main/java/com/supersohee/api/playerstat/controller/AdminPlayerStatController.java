package com.supersohee.api.playerstat.controller;

import com.supersohee.api.playerstat.domain.PlayerStat;
import com.supersohee.api.playerstat.dto.PlayerStatRequest;
import com.supersohee.api.playerstat.dto.PlayerStatResponse;
import com.supersohee.api.playerstat.service.PlayerStatService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin/playerstat")
@RequiredArgsConstructor
public class AdminPlayerStatController {

    private final PlayerStatService playerStatService;

    // 스탯 생성
    @PostMapping
    public ResponseEntity<Map<String, Object>> createPlayerStat(
            @AuthenticationPrincipal String userId,
            @Valid @RequestBody PlayerStatRequest request) {

        // 어드민 권한 체크
        if (!"admin".equals(userId)) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "어드민 권한이 필요합니다");
            return ResponseEntity.status(403).body(new HashMap<>(error));
        }

        try {
            PlayerStat playerStat = playerStatService.createPlayerStat(request);
            PlayerStatResponse response = PlayerStatResponse.from(playerStat);

            Map<String, Object> result = new HashMap<>();
            result.put("message", "스탯이 생성되었습니다");
            result.put("playerStat", response);

            return ResponseEntity.ok(result);
        } catch (RuntimeException e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "스탯 생성 중 오류 발생: " + e.getMessage());
            return ResponseEntity.status(500).body(error);
        }
    }

    // 스탯 수정
    @PutMapping("/{statId}")
    public ResponseEntity<Map<String, Object>> updatePlayerStat(
            @AuthenticationPrincipal String userId,
            @PathVariable String statId,
            @Valid @RequestBody PlayerStatRequest request) {

        // 어드민 권한 체크
        if (!"admin".equals(userId)) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "어드민 권한이 필요합니다");
            return ResponseEntity.status(403).body(new HashMap<>(error));
        }

        try {
            PlayerStat playerStat = playerStatService.updatePlayerStat(statId, request);
            PlayerStatResponse response = PlayerStatResponse.from(playerStat);

            Map<String, Object> result = new HashMap<>();
            result.put("message", "스탯이 수정되었습니다");
            result.put("playerStat", response);

            return ResponseEntity.ok(result);
        } catch (RuntimeException e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "스탯 수정 중 오류 발생: " + e.getMessage());
            return ResponseEntity.status(500).body(error);
        }
    }

    // 스탯 삭제
    @DeleteMapping("/{statId}")
    public ResponseEntity<Map<String, String>> deletePlayerStat(
            @AuthenticationPrincipal String userId,
            @PathVariable String statId) {

        // 어드민 권한 체크
        if (!"admin".equals(userId)) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "어드민 권한이 필요합니다");
            return ResponseEntity.status(403).body(error);
        }

        try {
            playerStatService.deletePlayerStat(statId);
            Map<String, String> result = new HashMap<>();
            result.put("message", "스탯이 삭제되었습니다");
            return ResponseEntity.ok(result);
        } catch (RuntimeException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    // 모든 스탯 조회 (어드민용)
    @GetMapping
    public ResponseEntity<List<PlayerStatResponse>> getAllPlayerStats(
            @AuthenticationPrincipal String userId) {

        // 어드민 권한 체크
        if (!"admin".equals(userId)) {
            return ResponseEntity.status(403).build();
        }

        List<PlayerStat> stats = playerStatService.findAllSoheeStats();
        List<PlayerStatResponse> responses = stats.stream()
                .map(PlayerStatResponse::from)
                .collect(Collectors.toList());

        return ResponseEntity.ok(responses);
    }

    // 특정 스탯 조회 (어드민용)
    @GetMapping("/{statId}")
    public ResponseEntity<PlayerStatResponse> getPlayerStat(
            @AuthenticationPrincipal String userId,
            @PathVariable String statId) {

        // 어드민 권한 체크
        if (!"admin".equals(userId)) {
            return ResponseEntity.status(403).build();
        }

        return playerStatService.findById(statId)
                .map(PlayerStatResponse::from)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
