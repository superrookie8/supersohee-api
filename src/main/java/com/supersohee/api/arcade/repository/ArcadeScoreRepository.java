package com.supersohee.api.arcade.repository;

import com.supersohee.api.arcade.domain.ArcadeScore;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;
import java.util.Optional;

public interface ArcadeScoreRepository extends MongoRepository<ArcadeScore, String> {
    // 유저별 점수 조회
    Optional<ArcadeScore> findByUserId(String userId);
    
    // 전체 랭킹 조회 (최고점순)
    List<ArcadeScore> findAllByOrderByBestScoreDesc();
    
    // 상위 N명 랭킹
    List<ArcadeScore> findTop10ByOrderByBestScoreDesc();
}
