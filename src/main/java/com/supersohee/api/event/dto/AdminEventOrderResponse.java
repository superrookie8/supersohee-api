package com.supersohee.api.event.dto;

import java.util.List;

public record AdminEventOrderResponse(String message, List<String> eventIds) {
}
