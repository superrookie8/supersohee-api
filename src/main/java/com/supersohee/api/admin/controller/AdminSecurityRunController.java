package com.supersohee.api.admin.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.supersohee.api.admin.error.AdminApiException;
import com.supersohee.api.admin.security.*;

import lombok.RequiredArgsConstructor;

import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/security/runs")
@RequiredArgsConstructor
public class AdminSecurityRunController {
    private final SecurityAuditService service;

    @PostMapping
    public ResponseEntity<SecurityAuditRun> execute(
            @RequestBody(required = false) JsonNode body, Authentication auth) {
        if (body != null && (!body.isObject() || !body.isEmpty()))
            throw AdminApiException.badRequest(
                    "Only an empty request body or empty object is supported.");
        return ResponseEntity.status(201)
                .cacheControl(CacheControl.noStore().cachePrivate())
                .body(service.execute(auth));
    }

    @GetMapping
    public ResponseEntity<Runs> list(@RequestParam(defaultValue = "20") int limit) {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore().cachePrivate())
                .body(new Runs(service.list(limit)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<SecurityAuditRun> get(@PathVariable String id) {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore().cachePrivate())
                .body(service.get(id));
    }

    public record Runs(List<SecurityAuditRun> runs) {}
}
