package com.supersohee.api.stadium.dto;

import lombok.*;
import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ZoneInfo {
    private String zoneName;
    private String seatType;
    private String floor;
    private List<BlockInfo> blocks;  // null 가능 (블럭이 없는 경우)
    private List<RowInfo> rows;      // blocks가 null일 때 사용
}

