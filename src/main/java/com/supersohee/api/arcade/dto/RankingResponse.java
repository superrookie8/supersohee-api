package com.supersohee.api.arcade.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RankingResponse {
    private List<RankingEntry> rankings;
    private Integer totalCount;
    private Integer myRank; // 현재 사용자의 랭킹 (null 가능)

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    /**
     * 공개 랭킹 항목. /api/arcade/ranking은 비로그인에도 열려 있으므로
     * 화면이 그리는 값만 담는다. userId와 profileImageUrl(구글 계정 사진 URL)은
     * 어떤 소비자도 쓰지 않으면서 개인정보만 노출해 제거했다.
     */
    public static class RankingEntry {
        private Integer rank;
        private String nickname;
        private Integer bestScore;
    }
}
