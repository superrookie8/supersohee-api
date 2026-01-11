package com.supersohee.api.diary.controller;

import com.supersohee.api.diary.domain.Diary;
import com.supersohee.api.diary.dto.DiaryRequest;
import com.supersohee.api.diary.dto.DiaryResponse;
import com.supersohee.api.diary.service.DiaryService;
import com.supersohee.api.image.service.ImageUploadService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/diary")
@RequiredArgsConstructor
public class DiaryController {

    private final DiaryService diaryService;
    private final ImageUploadService imageUploadService;

    // 이미지 키를 서명된 URL로 변환하는 헬퍼 메서드
    private DiaryResponse convertImageKeysToUrls(DiaryResponse response) {
        if (response.getPhotoUrls() != null && !response.getPhotoUrls().isEmpty()) {
            // photoUrls에 키가 저장되어 있으므로 서명된 URL로 변환
            List<String> presignedUrls = imageUploadService.convertKeysToPresignedUrls(
                    response.getPhotoUrls());
            return response.toBuilder()
                    .photoUrls(presignedUrls)
                    .build();
        }
        return response;
    }

    // 직관일지 작성 (인증 필요)
    @PostMapping
    public ResponseEntity<DiaryResponse> createDiary(
            @AuthenticationPrincipal String userId,
            @Valid @RequestBody DiaryRequest request) {
        // gameResult 검증 (null이 아니면 "승" 또는 "패"만 허용)
        validateGameResult(request.getGameResult());
        
        Diary diary = diaryService.createDiary(userId, request);
        // 작성 시에는 키 그대로 저장되므로 변환 불필요
        return ResponseEntity.ok(DiaryResponse.from(diary));
    }

    // 내가 쓴 직관일지 목록 조회 (인증 필요)
    @GetMapping
    public ResponseEntity<List<DiaryResponse>> getMyDiaries(
            @AuthenticationPrincipal String userId) {
        List<Diary> diaries = diaryService.findMyDiaries(userId);
        List<DiaryResponse> responses = diaries.stream()
                .map(DiaryResponse::from)
                .map(this::convertImageKeysToUrls) // 키를 서명된 URL로 변환
                .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }

    // 특정 직관일지 조회 (인증 필요, 본인 것만)
    @GetMapping("/{diaryId}")
    public ResponseEntity<DiaryResponse> getDiary(
            @AuthenticationPrincipal String userId,
            @PathVariable String diaryId) {
        return diaryService.findById(diaryId)
                .map(diary -> {
                    // 본인 일지인지 확인
                    if (!diary.getUserId().equals(userId)) {
                        throw new RuntimeException("본인의 일지만 조회할 수 있습니다");
                    }
                    return convertImageKeysToUrls(DiaryResponse.from(diary)); // 키를 서명된 URL로 변환
                })
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // 특정 경기의 내 일지 조회 (인증 필요)
    @GetMapping("/game/{gameId}")
    public ResponseEntity<DiaryResponse> getDiaryByGame(
            @AuthenticationPrincipal String userId,
            @PathVariable String gameId) {
        return diaryService.findByUserIdAndGameId(userId, gameId)
                .map(DiaryResponse::from)
                .map(this::convertImageKeysToUrls) // 키를 서명된 URL로 변환
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // 직관일지 수정 (인증 필요, 본인 것만)
    @PutMapping("/{diaryId}")
    public ResponseEntity<DiaryResponse> updateDiary(
            @AuthenticationPrincipal String userId,
            @PathVariable String diaryId,
            @Valid @RequestBody DiaryRequest request) {
        // gameResult 검증 (null이 아니면 "승" 또는 "패"만 허용)
        validateGameResult(request.getGameResult());
        
        Diary diary = diaryService.updateDiary(userId, diaryId, request);
        // 수정 시에는 키 그대로 저장되므로 변환 불필요
        return ResponseEntity.ok(DiaryResponse.from(diary));
    }
    
    // gameResult 검증 헬퍼 메서드
    private void validateGameResult(String gameResult) {
        if (gameResult != null && !gameResult.isEmpty()) {
            if (!gameResult.equals("승") && !gameResult.equals("패")) {
                throw new IllegalArgumentException("경기 결과는 '승' 또는 '패'만 가능합니다. (무승부 없음)");
            }
        }
    }

    // 직관일지 삭제 (인증 필요, 본인 것만)
    @DeleteMapping("/{diaryId}")
    public ResponseEntity<Void> deleteDiary(
            @AuthenticationPrincipal String userId,
            @PathVariable String diaryId) {
        diaryService.deleteDiary(userId, diaryId);
        return ResponseEntity.noContent().build();
    }
}