package com.supersohee.api.stadium.repository;

import com.supersohee.api.stadium.domain.Stadium;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;
import java.util.Optional;

public interface StadiumRepository extends MongoRepository<Stadium, String> {
    // 경기장 목록 조회 (이름순)
    List<Stadium> findAllByOrderByNameAsc();

    // 이름으로 경기장 조회
    Optional<Stadium> findByName(String name);
}
