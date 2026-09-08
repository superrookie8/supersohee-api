package com.supersohee.api.admin.security;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.List;

@Document(collection = "security_audit_runs")
public record SecurityAuditRun(
        @Id String id,
        int schemaVersion,
        Instant startedAt,
        Instant finishedAt,
        long durationMs,
        EnvironmentInfo environment,
        Summary summary,
        List<Check> checks,
        Instant expiresAt) {
    public record EnvironmentInfo(String backendMode, String frontendTarget) {}

    public record Summary(long pass, long warn, long fail, long unknown) {}

    public record Check(
            String id,
            String category,
            String title,
            String status,
            String evidenceType,
            String evidence,
            String remediation,
            Instant checkedAt,
            long durationMs,
            Integer observedStatus) {}
}
