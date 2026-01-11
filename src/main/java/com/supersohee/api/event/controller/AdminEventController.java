package com.supersohee.api.event.controller;

import com.supersohee.api.event.domain.Event;
import com.supersohee.api.event.dto.EventDetailsResponse;
import com.supersohee.api.event.service.EventService;
import com.supersohee.api.image.service.ImageUploadService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/events")
@RequiredArgsConstructor
public class AdminEventController {

    private final EventService eventService;
    private final ImageUploadService imageUploadService;

    // 이벤트 생성
    @PostMapping
    public ResponseEntity<Map<String, Object>> createEvent(
            @AuthenticationPrincipal String userId,
            @RequestParam("title") String title,
            @RequestParam(value = "url", required = false) String url,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam(value = "check_1", required = false) String check1,
            @RequestParam(value = "check_2", required = false) String check2,
            @RequestParam(value = "check_3", required = false) String check3,
            @RequestParam(value = "photos", required = false) List<MultipartFile> photos,
            @RequestParam(value = "isActive", defaultValue = "true") Boolean isActive) {
        
        // 어드민 권한 체크
        if (!"admin".equals(userId)) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "어드민 권한이 필요합니다");
            return ResponseEntity.status(403).body(new HashMap<>(error));
        }
        
        try {
            // 이미지 업로드 (있는 경우) - 이벤트 이미지는 event/ 경로로 저장
            List<String> photoKeys = null;
            if (photos != null && !photos.isEmpty()) {
                photoKeys = imageUploadService.uploadEventImages(photos);
            }

            // 이벤트 생성
            Event event = eventService.createEvent(
                    title,
                    url,
                    description,
                    check1,
                    check2,
                    check3,
                    photoKeys,
                    isActive
            );

            // 응답 생성 (상세 정보 포함)
            EventDetailsResponse response = EventDetailsResponse.from(event);
            if (photoKeys != null && !photoKeys.isEmpty()) {
                List<String> presignedUrls = imageUploadService.convertKeysToPresignedUrls(photoKeys);
                response = response.toBuilder()
                        .photos(presignedUrls)
                        .photoKeys(photoKeys)
                        .build();
            }

            Map<String, Object> result = new HashMap<>();
            result.put("message", "이벤트가 생성되었습니다");
            result.put("event", response);

            return ResponseEntity.ok(result);
        } catch (IOException e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "이미지 업로드 실패: " + e.getMessage());
            return ResponseEntity.badRequest().body(error);
        } catch (IllegalArgumentException e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "이벤트 생성 중 오류 발생: " + e.getMessage());
            return ResponseEntity.status(500).body(error);
        }
    }

    // 이벤트 수정
    @PutMapping("/{eventId}")
    public ResponseEntity<Map<String, Object>> updateEvent(
            @AuthenticationPrincipal String userId,
            @PathVariable String eventId,
            @RequestParam(value = "title", required = false) String title,
            @RequestParam(value = "url", required = false) String url,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam(value = "check_1", required = false) String check1,
            @RequestParam(value = "check_2", required = false) String check2,
            @RequestParam(value = "check_3", required = false) String check3,
            @RequestParam(value = "photos", required = false) List<MultipartFile> photos,
            @RequestParam(value = "isActive", required = false) Boolean isActive) {
        
        // 어드민 권한 체크
        if (!"admin".equals(userId)) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "어드민 권한이 필요합니다");
            return ResponseEntity.status(403).body(new HashMap<>(error));
        }
        
        try {
            // 이미지 업로드 (새로 추가된 경우) - 이벤트 이미지는 event/ 경로로 저장
            List<String> newPhotoKeys = null;
            if (photos != null && !photos.isEmpty()) {
                newPhotoKeys = imageUploadService.uploadEventImages(photos);
            }

            // 이벤트 수정
            Event event = eventService.updateEvent(
                    eventId,
                    title,
                    url,
                    description,
                    check1,
                    check2,
                    check3,
                    newPhotoKeys,
                    isActive
            );

            // 응답 생성
            EventDetailsResponse response = EventDetailsResponse.from(event);
            if (event.getPhotoKeys() != null && !event.getPhotoKeys().isEmpty()) {
                List<String> presignedUrls = imageUploadService.convertKeysToPresignedUrls(event.getPhotoKeys());
                response = response.toBuilder()
                        .photos(presignedUrls)
                        .photoKeys(event.getPhotoKeys())
                        .build();
            }

            Map<String, Object> result = new HashMap<>();
            result.put("message", "이벤트가 수정되었습니다");
            result.put("event", response);

            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        } catch (IOException e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "이미지 업로드 실패: " + e.getMessage());
            return ResponseEntity.badRequest().body(error);
        } catch (RuntimeException e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "이벤트 수정 중 오류 발생: " + e.getMessage());
            return ResponseEntity.status(500).body(error);
        }
    }

    // 이벤트 사진 개별 삭제 (쿼리 파라미터 방식)
    @DeleteMapping("/{eventId}/photos")
    public ResponseEntity<Map<String, Object>> deleteEventPhoto(
            @AuthenticationPrincipal String userId,
            @PathVariable String eventId,
            @RequestParam("photoKey") String photoKey) {
        
        // 어드민 권한 체크
        if (!"admin".equals(userId)) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "어드민 권한이 필요합니다");
            return ResponseEntity.status(403).body(new HashMap<>(error));
        }
        
        try {
            // R2에서 이미지 삭제
            imageUploadService.deleteImage(photoKey);
            
            // 이벤트에서 사진 키 제거
            Event event = eventService.deleteEventPhoto(eventId, photoKey);
            
            // 응답 생성
            EventDetailsResponse response = EventDetailsResponse.from(event);
            if (event.getPhotoKeys() != null && !event.getPhotoKeys().isEmpty()) {
                List<String> presignedUrls = imageUploadService.convertKeysToPresignedUrls(event.getPhotoKeys());
                response = response.toBuilder()
                        .photos(presignedUrls)
                        .photoKeys(event.getPhotoKeys())
                        .build();
            }
            
            Map<String, Object> result = new HashMap<>();
            result.put("message", "사진이 삭제되었습니다");
            result.put("event", response);
            
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        } catch (RuntimeException e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "사진 삭제 중 오류 발생: " + e.getMessage());
            return ResponseEntity.status(500).body(error);
        }
    }

    // 이벤트 삭제 (isActive = false로 설정)
    @DeleteMapping("/{eventId}")
    public ResponseEntity<Map<String, String>> deleteEvent(
            @AuthenticationPrincipal String userId,
            @PathVariable String eventId) {
        
        // 어드민 권한 체크
        if (!"admin".equals(userId)) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "어드민 권한이 필요합니다");
            return ResponseEntity.status(403).body(error);
        }
        
        try {
            eventService.deleteEvent(eventId);
            Map<String, String> result = new HashMap<>();
            result.put("message", "이벤트가 삭제되었습니다");
            return ResponseEntity.ok(result);
        } catch (RuntimeException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    // 모든 이벤트 조회 (어드민용, 비활성 포함)
    @GetMapping
    public ResponseEntity<List<EventDetailsResponse>> getAllEvents(
            @AuthenticationPrincipal String userId) {
        
        // 어드민 권한 체크
        if (!"admin".equals(userId)) {
            return ResponseEntity.status(403).build();
        }
        
        List<Event> events = eventService.findAllEvents();
        List<EventDetailsResponse> responses = events.stream()
                .map(event -> {
                    EventDetailsResponse response = EventDetailsResponse.from(event);
                    if (event.getPhotoKeys() != null && !event.getPhotoKeys().isEmpty()) {
                        List<String> presignedUrls = imageUploadService
                                .convertKeysToPresignedUrls(event.getPhotoKeys());
                        response = response.toBuilder()
                                .photos(presignedUrls)
                                .photoKeys(event.getPhotoKeys())
                                .build();
                    }
                    return response;
                })
                .toList();
        
        return ResponseEntity.ok(responses);
    }
}
