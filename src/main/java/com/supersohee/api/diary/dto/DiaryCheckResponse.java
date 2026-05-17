package com.supersohee.api.diary.dto;

import com.supersohee.api.diary.domain.Diary;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 캘린더/일정 선택 시 일지 존재 여부 확인용 응답.
 * 프론트: exists=true → "이미 작성한 일지가 있습니다. 수정하시겠습니까?" 모달 후 diaryId로 수정 페이지 이동
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DiaryCheckResponse {

    private boolean exists;
    private String diaryId;
    private DiaryResponse diary;

    public static DiaryCheckResponse notFound() {
        return DiaryCheckResponse.builder()
                .exists(false)
                .build();
    }

    public static DiaryCheckResponse found(Diary diary) {
        DiaryResponse response = DiaryResponse.from(diary);
        return DiaryCheckResponse.builder()
                .exists(true)
                .diaryId(diary.getId())
                .diary(response)
                .build();
    }
}
