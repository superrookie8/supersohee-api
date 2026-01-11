package com.supersohee.api.game.dto;

import com.supersohee.api.game.domain.Game;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GameResponse {

    private String id;
    private String season;
    private String league;
    private String homeTeam;
    private String awayTeam;
    private LocalDateTime gameDateTime;
    private String stadiumId;

    // BNK SUM 기준 홈/어웨이 구분
    private Boolean isHomeGame; // true: 홈경기 (붉은색), false: 어웨이경기 (푸른색)

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static GameResponse from(Game game) {
        // BNK SUM 팀 기준으로 홈경기 여부 판단
        boolean isHome = "BNK SUM".equals(game.getHomeTeam()) ||
                "BNK 썸".equals(game.getHomeTeam());

        return GameResponse.builder()
                .id(game.getId())
                .season(game.getSeason())
                .league(game.getLeague())
                .homeTeam(game.getHomeTeam())
                .awayTeam(game.getAwayTeam())
                .gameDateTime(game.getGameDateTime())
                .stadiumId(game.getStadiumId())
                .isHomeGame(isHome)
                .createdAt(game.getCreatedAt())
                .updatedAt(game.getUpdatedAt())
                .build();
    }
}
