package com.supersohee.api.playerstat.repository;

import com.supersohee.api.playerstat.domain.PlayerStat;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.Optional;
import java.util.List;

public interface PlayerStatRepository extends MongoRepository<PlayerStat, String> {
     // 시즌별 통계 조회 (이소희 선수 통계만)
     Optional<PlayerStat> findBySeason(String season);

     // 모든 시즌 통계 조회 (최신순)
     List<PlayerStat> findAllByOrderBySeasonDesc();
}
