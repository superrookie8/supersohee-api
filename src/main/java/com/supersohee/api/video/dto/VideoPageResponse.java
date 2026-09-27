package com.supersohee.api.video.dto;

public record VideoPageResponse(
        java.util.List<VideoResponse> videos,
        long total,
        int page,
        int limit,
        int totalPages,
        boolean hasNext,
        boolean hasPrevious) {}
