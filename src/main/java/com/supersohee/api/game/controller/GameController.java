package com.supersohee.api.game.controller;

import com.supersohee.api.game.domain.Game;
import com.supersohee.api.game.dto.GameResponse;
import com.supersohee.api.game.service.GameService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/games")
@RequiredArgsConstructor
public class GameController {

    private final GameService gameService;

    @GetMapping
    public ResponseEntity<List<GameResponse>> getGames(
            @RequestParam(required = false) String season,
            @RequestParam(required = false) String stadiumId) {

        List<Game> games;

        if (stadiumId != null) {
            games = gameService.findByStadiumId(stadiumId);
        } else if (season != null) {
            games = gameService.findBySeason(season);
        } else {
            games = List.of();
        }

        List<GameResponse> responses = games.stream()
                .map(game -> toResponse(game))
                .collect(Collectors.toList());

        return ResponseEntity.ok(responses);
    }

    @GetMapping("/{gameId}")
    public ResponseEntity<GameResponse> getGame(@PathVariable String gameId) {
        return gameService.findById(gameId)
                .map(this::toResponse)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    private GameResponse toResponse(Game game) {
        boolean isHomeGame = gameService.isHomeGame(game);

        return GameResponse.builder()
                .id(game.getId())
                .season(game.getSeason())
                .league(game.getLeague())
                .homeTeam(game.getHomeTeam())
                .awayTeam(game.getAwayTeam())
                .gameDateTime(game.getGameDateTime())
                .stadiumId(game.getStadiumId())
                .isHomeGame(isHomeGame)
                .createdAt(game.getCreatedAt())
                .updatedAt(game.getUpdatedAt())
                .build();
    }
}