package com.supersohee.api.game.domain;

import lombok.*;
import com.supersohee.api.common.enums.GameResultType;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class GameResult {
   
        private Integer homeScore;
        private Integer awayScore;
        private GameResultType winner;          // HOME | AWAY
    
}
