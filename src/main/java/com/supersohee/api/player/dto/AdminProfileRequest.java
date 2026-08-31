package com.supersohee.api.player.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record AdminProfileRequest(
        @NotBlank String name,
        @NotBlank String team,
        @NotBlank String position,
        @NotNull @Min(6) @Max(6) @JsonAlias("number") Integer jerseyNumber,
        @NotBlank String height,
        @NotNull @JsonAlias("nickname")
        @JsonFormat(with = JsonFormat.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
        List<String> nicknames,
        String features,
        String profileImageUrl) {
}
