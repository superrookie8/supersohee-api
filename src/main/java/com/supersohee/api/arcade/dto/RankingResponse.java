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
    public static class RankingEntry {
        private Integer rank;
        private String userId;
        private String nickname;
        private String profileImageUrl;
        private Integer bestScore;
    }
}
