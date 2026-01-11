package com.supersohee.api.event.repository;

import com.supersohee.api.event.domain.Event;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;
import java.util.Optional;

public interface EventRepository extends MongoRepository<Event, String> {
    List<Event> findByIsActiveTrueOrderByCreatedAtDesc();
    Optional<Event> findByIdAndIsActiveTrue(String id);
    
    // 어드민용: 모든 이벤트 조회 (비활성 포함)
    List<Event> findAllByOrderByCreatedAtDesc();
}

