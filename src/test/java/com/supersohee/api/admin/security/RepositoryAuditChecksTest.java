package com.supersohee.api.admin.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;
import java.net.*;
import java.time.Instant;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

class RepositoryAuditChecksTest {
    final ObjectMapper mapper = new ObjectMapper();
    RepositoryAuditHttp.Reply reply(String json) throws Exception { return new RepositoryAuditHttp.Reply(200, mapper.readTree(json), false); }
    @Test void executionStateAndMissingOrContaminatedResponses() throws Exception {
        assertThat(RepositoryAuditChecks.actions("id", reply("{\"workflow_runs\":[{\"status\":\"completed\",\"conclusion\":\"success\"}]}" )).status()).isEqualTo("pass");
        assertThat(RepositoryAuditChecks.actions("id", reply("{\"workflow_runs\":[{\"status\":\"completed\",\"conclusion\":\"failure\"}]}" )).status()).isEqualTo("fail");
        for (String body : List.of("{}", "{\"workflow_runs\":[]}", "{\"workflow_runs\":[{\"status\":\"SECRET_SENTINEL\"}]}")) {
            var result = RepositoryAuditChecks.actions("id", reply(body));
            assertThat(result.status()).isEqualTo("unknown");
            assertThat(result.toString()).doesNotContain("SECRET_SENTINEL");
        }
        for (int code : List.of(301, 403, 429, 500)) assertThat(RepositoryAuditChecks.actions("id", new RepositoryAuditHttp.Reply(code, null, false)).status()).isEqualTo("unknown");
    }
    @Test void scheduledStaleMissingFutureAndRecentTimesAreDistinct() throws Exception {
        Instant now = Instant.parse("2026-09-09T00:00:00Z");
        for (var item : Map.of("2026-08-01T00:00:00Z", "warn", "2026-09-08T00:00:00Z", "pass", "2026-10-01T00:00:00Z", "unknown", "bad", "unknown").entrySet()) {
            var response = reply("{\"workflow_runs\":[{\"status\":\"completed\",\"conclusion\":\"success\",\"created_at\":\"" + item.getKey() + "\"}]}");
            assertThat(RepositoryAuditChecks.scheduled(response, now).status()).isEqualTo(item.getValue());
        }
    }
    @Test void alertPagesAndSeverityNeverEchoProviderFields() throws Exception {
        var body = reply("[{\"state\":\"open\",\"security_advisory\":{\"severity\":\"high\",\"summary\":\"SECRET_SENTINEL\"}}]");
        var result = RepositoryAuditChecks.alerts("id", body);
        assertThat(result.status()).isEqualTo("fail");
        assertThat(result.toString()).contains("high=1").doesNotContain("SECRET_SENTINEL");
        assertThat(RepositoryAuditChecks.alerts("id", reply("[]")).status()).isEqualTo("pass");
        assertThat(RepositoryAuditChecks.alerts("id", new RepositoryAuditHttp.Reply(200, mapper.readTree("[]"), true)).status()).isEqualTo("warn");
        assertThat(RepositoryAuditChecks.alerts("id", reply("{}" )).status()).isEqualTo("unknown");
    }
    @Test void absentTokenSkipsAlertsAndRequestsOnlyFixedPaths() throws Exception {
        var http = mock(RepositoryAuditHttp.class);
        when(http.get(anyString(), anyString())).thenReturn(new RepositoryAuditHttp.Reply(403, null, false));
        var checks = new ArrayList<SecurityAuditRun.Check>();
        new RepositoryAuditChecks(http, new MockEnvironment()).inspect(checks::add);
        assertThat(checks).hasSize(5).allMatch(c -> c.status().equals("unknown"));
        verify(http, never()).get(contains("dependabot"), anyString());
        verify(http).get("/repos/superrookie8/sofanpage/actions/workflows/supersohee-weekly-crawler.yml", "");
    }
    @Test void unauthorizedTokenAndExceptionsRemainPrivate() throws Exception {
        var http = mock(RepositoryAuditHttp.class);
        when(http.get(anyString(), anyString())).thenThrow(new IllegalStateException("SECRET_SENTINEL"));
        var checks = new ArrayList<SecurityAuditRun.Check>();
        new RepositoryAuditChecks(http, new MockEnvironment().withProperty("GITHUB_SECURITY_TOKEN", "SECRET_SENTINEL")).inspect(checks::add);
        assertThat(checks.toString()).doesNotContain("SECRET_SENTINEL");
        assertThat(checks).hasSize(5).allMatch(c -> c.status().equals("unknown"));
    }
    @Test void arbitraryHostAndPathAreRejectedBeforeTransport() {
        var http = new RepositoryAuditHttp();
        try {
            assertThatThrownBy(() -> http.get("/repos/attacker/repo/actions/runs?per_page=1", "SECRET_SENTINEL")).isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> RepositoryAuditHttp.fetch(URI.create("https://attacker.example/"), new InetAddress[0], "SECRET_SENTINEL")).isInstanceOf(IllegalArgumentException.class);
        } finally { http.shutdown(); }
    }
    @Test void boundedDecoderRejectsOversizeMalformedAndNeverReadsErrorOrRedirectBodies() throws Exception {
        assertThatThrownBy(() -> RepositoryAuditHttp.decode(200, new java.io.ByteArrayInputStream(new byte[262145]), false))
                .isInstanceOf(java.io.IOException.class);
        assertThatThrownBy(() -> RepositoryAuditHttp.decode(200, new java.io.ByteArrayInputStream("SECRET_SENTINEL".getBytes()), false))
                .isInstanceOf(java.io.IOException.class);
        var body = mock(java.io.InputStream.class);
        for (int status : List.of(301, 302, 403, 429)) {
            assertThat(RepositoryAuditHttp.decode(status, body, true).body()).isNull();
        }
        verifyNoInteractions(body);
    }

}
