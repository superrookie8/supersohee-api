package com.supersohee.api.video.domain;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document("videos")
public record Video(
        @Id String id,
        String title,
        String channelName,
        String category,
        java.time.LocalDate eventDate,
        String eventKey,
        String eventLabel,
        String eventDateBasis,
        Boolean leeFocused,
        Integer displayOrder,
        Boolean published,
        String duration,
        String badge,
        java.time.LocalDate verifiedOn,
        String thumbnailUrl,
        java.time.LocalDateTime createdAt,
        java.time.LocalDateTime updatedAt) {}
