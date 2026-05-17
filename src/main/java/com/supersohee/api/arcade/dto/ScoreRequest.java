package com.supersohee.api.arcade.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class ScoreRequest {

    @NotNull(message = "점수는 필수입니다")
    @Min(value = 0, message = "점수는 0 이상이어야 합니다")
    private Integer score;
}
