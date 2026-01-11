package com.supersohee.api.event.controller;

import com.supersohee.api.event.domain.Event;
import com.supersohee.api.event.dto.EventResponse;
import com.supersohee.api.event.dto.EventDetailsResponse;
import com.supersohee.api.event.service.EventService;
import com.supersohee.api.image.service.ImageUploadService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/events")
@RequiredArgsConstructor
public class EventController {

    private final EventService eventService;
    private final ImageUploadService imageUploadService;

    // 이벤트 목록 조회 (공개)
    @GetMapping
    public ResponseEntity<List<EventResponse>> getEvents() {
        List<Event> events = eventService.findActiveEvents();
        List<EventResponse> responses = events.stream()
                .map(EventResponse::from)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }

    // 이벤트 상세 조회 (공개)
    @GetMapping("/{eventId}")
    public ResponseEntity<EventDetailsResponse> getEventDetails(
            @PathVariable String eventId) {
        return eventService.findById(eventId)
                .map(event -> {
                    EventDetailsResponse response = EventDetailsResponse.from(event);
                    
                    // R2 키를 presigned URL로 변환
                    if (event.getPhotoKeys() != null && !event.getPhotoKeys().isEmpty()) {
                        List<String> presignedUrls = imageUploadService
                                .convertKeysToPresignedUrls(event.getPhotoKeys());
                        response = response.toBuilder()
                                .photos(presignedUrls)
                                .build();
                    }
                    
                    return ResponseEntity.ok(response);
                })
                .orElse(ResponseEntity.notFound().build());
    }
}

