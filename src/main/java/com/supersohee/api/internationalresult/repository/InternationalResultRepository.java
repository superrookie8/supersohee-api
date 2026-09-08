package com.supersohee.api.internationalresult.repository;

import com.supersohee.api.internationalresult.domain.InternationalResult;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface InternationalResultRepository extends MongoRepository<InternationalResult, String> {
    List<InternationalResult> findByPublishedTrueOrderByDisplayOrderAscStartDateDesc();
    List<InternationalResult> findAllByOrderByDisplayOrderAscStartDateDesc();
    Optional<InternationalResult> findByCompetitionKey(String competitionKey);
}
