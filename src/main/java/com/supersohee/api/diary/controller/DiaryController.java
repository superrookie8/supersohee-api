package com.supersohee.api.diary.controller;

import com.supersohee.api.diary.domain.Diary;
import com.supersohee.api.diary.dto.DiaryCheckResponse;
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
import java.util.Optional;
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
        return ResponseEntity.ok(diaryService.toDiaryResponse(diary));
    }

    // 내가 쓴 직관일지 목록 조회 (인증 필요)
    @GetMapping
    public ResponseEntity<List<DiaryResponse>> getMyDiaries(
            @AuthenticationPrincipal String userId) {
        List<Diary> diaries = diaryService.findMyDiaries(userId);
        List<DiaryResponse> responses = diaries.stream()
                .map(diaryService::toDiaryResponse)
                .map(this::convertImageKeysToUrls)
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
                    return convertImageKeysToUrls(diaryService.toDiaryResponse(diary));
                })
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * 캘린더에서 경기(일정) 선택 시 호출 — POST 전에 일지 존재 여부 확인.
     * exists=true → 모달("이미 작성한 일지가 있습니다. 수정하시겠습니까?") 후 diaryId로 수정 페이지
     * exists=false → 작성 페이지로 이동
     */
    @GetMapping("/game/{gameId}")
    public ResponseEntity<DiaryCheckResponse> getDiaryByGame(
            @AuthenticationPrincipal String userId,
            @PathVariable String gameId) {
        return diaryService.findByUserIdAndGameId(userId, gameId)
                .map(diary -> toDiaryCheckResponse(diary))
                .map(check -> convertCheckImageUrls(check))
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.ok(DiaryCheckResponse.notFound()));
    }

    /**
     * 날짜만으로 일지 존재 여부 확인 (gameId 없이 날짜 선택 시).
     */
    @GetMapping("/date/{date}")
    public ResponseEntity<DiaryCheckResponse> getDiaryByDate(
            @AuthenticationPrincipal String userId,
            @PathVariable String date) {
        return diaryService.findByUserIdAndDate(userId, date)
                .map(diary -> toDiaryCheckResponse(diary))
                .map(check -> convertCheckImageUrls(check))
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.ok(DiaryCheckResponse.notFound()));
    }

    private DiaryCheckResponse toDiaryCheckResponse(Diary diary) {
        return DiaryCheckResponse.builder()
                .exists(true)
                .diaryId(diary.getId())
                .diary(diaryService.toDiaryResponse(diary))
                .build();
    }

    private DiaryCheckResponse convertCheckImageUrls(DiaryCheckResponse check) {
        if (check.getDiary() == null) {
            return check;
        }
        return DiaryCheckResponse.builder()
                .exists(check.isExists())
                .diaryId(check.getDiaryId())
                .diary(convertImageKeysToUrls(check.getDiary()))
                .build();
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
        return ResponseEntity.ok(diaryService.toDiaryResponse(diary));
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