package com.supersohee.api.video.controller;

import com.supersohee.api.video.dto.*;
import com.supersohee.api.video.service.VideoService;

import lombok.RequiredArgsConstructor;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/videos")
@RequiredArgsConstructor
public class VideoController {
    private final VideoService service;

    @GetMapping
    public VideoPageResponse list(
            @RequestParam(defaultValue = "all") String category,
            @RequestParam(defaultValue = "recent") String sort,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int limit) {
        return service.list(false, category, sort, page, limit);
    }
}
