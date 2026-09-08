package com.supersohee.api.internationalresult.controller;

import com.supersohee.api.internationalresult.dto.InternationalResultResponse;
import com.supersohee.api.internationalresult.service.InternationalResultService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/international-results")
@RequiredArgsConstructor
public class InternationalResultController {
    private final InternationalResultService service;

    @GetMapping
    public List<InternationalResultResponse> getPublishedResults() {
        return service.findPublished().stream().map(InternationalResultResponse::from).toList();
    }
}
