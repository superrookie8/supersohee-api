package com.supersohee.api.arcade.controller;

import com.supersohee.api.arcade.dto.RankingResponse;
import com.supersohee.api.arcade.dto.ScoreRequest;
import com.supersohee.api.arcade.dto.ScoreResponse;
import com.supersohee.api.arcade.error.ArcadeApiException;
import com.supersohee.api.arcade.service.ArcadeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/arcade")
@RequiredArgsConstructor
public class ArcadeController {

    private final ArcadeService arcadeService;

    /**
     * 점수 제출
     * POST /api/arcade/score
     */
    @PostMapping("/score")
    public ResponseEntity<ScoreResponse> submitScore(
            @AuthenticationPrincipal String userId,
            @Valid @RequestBody ScoreRequest request) {
        ScoreResponse response = arcadeService.submitScore(userId, request.getScore());
        return ResponseEntity.ok(response);
    }

    /**
     * 내 최고 점수 조회
     * GET /api/arcade/my-score
     */
    @GetMapping("/my-score")
    public ResponseEntity<ScoreResponse> getMyScore(
            @AuthenticationPrincipal String userId) {
        ScoreResponse response = arcadeService.getMyScore(userId);
        return ResponseEntity.ok(response);
    }

    /**
     * 랭킹 조회
     * GET /api/arcade/ranking?limit=10
     * limit 파라미터가 없으면 전체 랭킹 반환
     */
    @GetMapping("/ranking")
    public ResponseEntity<RankingResponse> getRanking(
            @RequestParam(required = false) Integer limit,
            Authentication authentication) {
        if (limit != null && (limit < 1 || limit > 100)) {
            throw ArcadeApiException.badRequest("limit must be between 1 and 100.");
        }
        String currentUserId = authentication == null || authentication instanceof AnonymousAuthenticationToken
                ? null
                : authentication.getName();
        RankingResponse response = arcadeService.getRanking(limit, currentUserId);
        return ResponseEntity.ok(response);
    }
}
