package com.supersohee.api.game.repository;

import com.supersohee.api.game.domain.Game;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.time.LocalDateTime;
import java.util.List;

public interface GameRepository extends MongoRepository<Game, String> {
    // 시즌별 경기 조회
    List<Game> findBySeasonOrderByGameDateTimeAsc(String season);

    // 경기장별 경기 조회
    List<Game> findByStadiumIdOrderByGameDateTimeAsc(String stadiumId);

    // 경기장 + 시간대(근접 범위)로 경기 조회 (스케줄↔직관일지 매칭용)
    List<Game> findByStadiumIdAndGameDateTimeBetweenOrderByGameDateTimeAsc(
            String stadiumId,
            LocalDateTime start,
            LocalDateTime end);

}
