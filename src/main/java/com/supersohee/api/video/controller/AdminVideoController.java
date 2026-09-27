package com.supersohee.api.video.controller;

import com.supersohee.api.video.dto.*;
import com.supersohee.api.video.service.VideoService;

import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/videos")
@RequiredArgsConstructor
public class AdminVideoController {
    private final VideoService service;

    @GetMapping
    public VideoPageResponse list(
            @RequestParam(defaultValue = "all") String category,
            @RequestParam(defaultValue = "recent") String sort,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int limit) {
        return service.list(true, category, sort, page, limit);
    }

    @PostMapping
    public ResponseEntity<VideoResponse> create(@RequestBody VideoRequest r) {
        return ResponseEntity.status(201).body(service.create(r));
    }

    @PutMapping("/{id}")
    public VideoResponse update(@PathVariable String id, @RequestBody VideoRequest r) {
        return service.update(id, r);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/import")
    public VideoImportResponse importVideos(@RequestBody VideoImportRequest r) {
        return service.importVideos(r);
    }
}
