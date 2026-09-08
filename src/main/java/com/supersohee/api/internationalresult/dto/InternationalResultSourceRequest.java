package com.supersohee.api.internationalresult.dto;

import com.supersohee.api.internationalresult.domain.InternationalResultSource;
import com.supersohee.api.internationalresult.domain.InternationalResultSourceType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record InternationalResultSourceRequest(
        @NotBlank @Size(max = 120) String label,
        @NotBlank @Size(max = 2048)
        @Pattern(regexp = "https://[^\\s]+", message = "must be an HTTPS URL") String url,
        @NotNull InternationalResultSourceType type) {

    public InternationalResultSource toDomain() {
        return InternationalResultSource.builder()
                .label(label.trim())
                .url(url.trim())
                .type(type)
                .build();
    }
}
