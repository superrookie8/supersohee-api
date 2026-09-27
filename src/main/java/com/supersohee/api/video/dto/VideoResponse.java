package com.supersohee.api.video.dto;

import com.supersohee.api.video.domain.Video;

public record VideoResponse(
        String id,
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
        String url,
        java.time.LocalDateTime createdAt,
        java.time.LocalDateTime updatedAt) {
    public static VideoResponse from(Video v) {
        return new VideoResponse(
                v.id(),
                v.title(),
                v.channelName(),
                v.category(),
                v.eventDate(),
                v.eventKey(),
                v.eventLabel(),
                v.eventDateBasis(),
                v.leeFocused(),
                v.displayOrder(),
                v.published(),
                v.duration(),
                v.badge(),
                v.verifiedOn(),
                v.thumbnailUrl(),
                "https://www.youtube.com/watch?v=" + v.id(),
                v.createdAt(),
                v.updatedAt());
    }
}
