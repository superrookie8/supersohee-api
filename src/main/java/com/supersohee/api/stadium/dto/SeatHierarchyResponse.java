package com.supersohee.api.stadium.dto;

import lombok.*;
import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SeatHierarchyResponse {
    private List<ZoneInfo> zones;
}

