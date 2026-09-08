package com.supersohee.api.internationalresult.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InternationalResultSource {
    private String label;
    private String url;
    private InternationalResultSourceType type;
}
