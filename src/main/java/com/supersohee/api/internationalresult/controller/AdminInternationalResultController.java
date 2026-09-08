package com.supersohee.api.internationalresult.controller;

import com.supersohee.api.internationalresult.dto.InternationalResultRequest;
import com.supersohee.api.internationalresult.dto.InternationalResultResponse;
import com.supersohee.api.internationalresult.service.InternationalResultService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/international-results")
@RequiredArgsConstructor
public class AdminInternationalResultController {
    private final InternationalResultService service;

    @GetMapping
    public List<InternationalResultResponse> getAllResults() {
        return service.findAllForAdmin().stream().map(InternationalResultResponse::from).toList();
    }

    @PostMapping
    public ResponseEntity<InternationalResultResponse> create(
            @Valid @RequestBody InternationalResultRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(InternationalResultResponse.from(service.create(request)));
    }

    @PutMapping("/{id}")
    public InternationalResultResponse update(
            @PathVariable String id,
            @Valid @RequestBody InternationalResultRequest request) {
        return InternationalResultResponse.from(service.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
