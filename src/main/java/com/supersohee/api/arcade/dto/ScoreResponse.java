package com.supersohee.api.arcade.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScoreResponse {
    private String userId;
    private String nickname;
    private String profileImageUrl;
    private Integer bestScore;
    private Integer rank; // 전체 랭킹에서의 순위 (null 가능)
}
