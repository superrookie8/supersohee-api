package com.supersohee.api.video.dto;

public record VideoRequest(
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
        String thumbnailUrl) {}
