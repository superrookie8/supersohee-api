package com.supersohee.api.video.repository;

import com.supersohee.api.video.domain.Video;

import org.springframework.data.mongodb.repository.MongoRepository;

public interface VideoRepository extends MongoRepository<Video, String> {
    java.util.List<Video> findByPublishedTrue();
}
