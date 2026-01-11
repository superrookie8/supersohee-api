package com.supersohee.api.game.repository;

import com.supersohee.api.game.domain.Game;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;

public interface GameRepository extends MongoRepository<Game, String> {
    // 시즌별 경기 조회
    List<Game> findBySeasonOrderByGameDateTimeAsc(String season);
    
    
    // 경기장별 경기 조회
    List<Game> findByStadiumIdOrderByGameDateTimeAsc(String stadiumId);
    
}
