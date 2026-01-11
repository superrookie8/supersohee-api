package com.supersohee.api.game.domain;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import com.supersohee.api.common.BaseDocument;
import java.time.LocalDateTime;


@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Document(collection = "games")
public class Game extends BaseDocument {



    @Id
    private String id;

    private String season;          // 2025-2026
    private String league;          // WKBL

    private String homeTeam;
    private String awayTeam;

    private LocalDateTime gameDateTime;

    private String stadiumId;       // 경기장 참조
    

}
    
