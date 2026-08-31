package com.supersohee.api.event.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record AdminEventOrderRequest(
        @NotNull(message = "eventIds is required")
        @Size(max = 500, message = "eventIds must contain at most 500 items")
        List<@NotBlank(message = "event id must not be blank") String> eventIds) {
}
