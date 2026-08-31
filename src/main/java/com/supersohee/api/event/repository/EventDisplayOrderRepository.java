package com.supersohee.api.event.repository;

import com.supersohee.api.event.domain.EventDisplayOrder;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface EventDisplayOrderRepository extends MongoRepository<EventDisplayOrder, String> {
}
