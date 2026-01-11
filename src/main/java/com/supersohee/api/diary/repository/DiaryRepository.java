package com.supersohee.api.diary.repository;

import com.supersohee.api.diary.domain.Diary;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;
import java.util.Optional;


public interface DiaryRepository extends MongoRepository<Diary, String> {
    // 마이페이지용: 내가 작성한 일지 목록 (최신순)
    List<Diary> findByUserIdOrderByCreatedAtDesc(String userId);

    // 캘린더용: 특정 경기의 유저 일지 조회
    Optional<Diary> findByUserIdAndGameId(String userId, String gameId);
        
    // 유저별 직관 승률 통계용 (DIRECT만)
    List<Diary> findByUserIdAndWatchType(String userId, String watchType);
    
}
