package com.supersohee.api.stadium.dto;

import lombok.*;
import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BlockInfo {
    private String blockName;
    private List<RowInfo> rows;
}

