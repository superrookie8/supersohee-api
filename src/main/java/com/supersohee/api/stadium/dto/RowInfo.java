package com.supersohee.api.stadium.dto;

import lombok.*;
import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RowInfo {
    private String row;
    private List<String> numbers;  // ["1번", "2번", "3번", ...]
}

