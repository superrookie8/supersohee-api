package com.supersohee.api.schedule.repository;

import com.supersohee.api.schedule.domain.Schedule;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ScheduleRepository extends MongoRepository<Schedule, String> {
    // 활성 스케줄만 조회 (날짜 범위)
    List<Schedule> findByIsActiveTrueAndStartDateTimeBetweenOrderByStartDateTimeAsc(
            LocalDateTime start, LocalDateTime end);
    
    // 활성 스케줄만 조회 (전체)
    List<Schedule> findByIsActiveTrueOrderByStartDateTimeAsc();
    
    // 어드민용: 모든 스케줄 조회
    List<Schedule> findAllByOrderByStartDateTimeDesc();
    
    // 어드민용: 특정 스케줄 조회
    Optional<Schedule> findById(String id);
}
