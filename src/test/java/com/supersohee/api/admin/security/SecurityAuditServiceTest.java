package com.supersohee.api.admin.security;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.supersohee.api.admin.error.AdminApiException;

import org.junit.jupiter.api.Test;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.time.*;
import java.util.*;

class SecurityAuditServiceTest {
    final MongoOperations mongo = mock(MongoOperations.class);
    final SecurityAuditProbe probe = mock(SecurityAuditProbe.class);
    final MockEnvironment env =
            new MockEnvironment()
                    .withProperty("jwt.secret", "SECRET_SENTINEL")
                    .withProperty("app.frontend.url", "https://SECRET_SENTINEL.example.com");
    final Instant now = Instant.parse("2026-09-08T12:00:00Z");
    final SecurityAuditService service =
            new SecurityAuditService(mongo, env, probe, Clock.fixed(now, ZoneOffset.UTC));

    SecurityAuditServiceTest() {
        when(probe.targetKind(any(), anyBoolean())).thenReturn("https-configured");
        when(probe.inspect(any(), anyBoolean())).thenReturn(List.of());
        when(mongo.find(any(Query.class), eq(SecurityAuditRun.class))).thenReturn(List.of());
    }

    @Test
    void partialFailuresAreStoredWithNoSensitiveValuesAndCooldownBlocksRepeat() {
        when(mongo.executeCommand(any(org.bson.Document.class)))
                .thenThrow(new RuntimeException("SECRET_SENTINEL"));
        var auth =
                new UsernamePasswordAuthenticationToken(
                        "PRIVATE_MEMBER", null, List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
        var run = service.execute(auth);
        assertThat(run.checks()).anyMatch(c -> c.status().equals("unknown"));
        assertThat(run.expiresAt()).isEqualTo(now.plus(Duration.ofDays(30)));
        assertThat(run.toString()).doesNotContain("SECRET_SENTINEL", "PRIVATE_MEMBER");
        verify(mongo)
                .save(argThat((SecurityAuditRun x) -> !x.toString().contains("SECRET_SENTINEL")));
        assertThatThrownBy(() -> service.execute(auth))
                .isInstanceOfSatisfying(
                        AdminApiException.class,
                        e -> assertThat(e.status().value()).isEqualTo(429));
        service.shutdown();
    }

    @Test
    void historyValidatesLimitsExpiryAndUnavailableStore() {
        assertThatThrownBy(() -> service.list(21)).isInstanceOf(AdminApiException.class);
        service.list(20);
        verify(mongo)
                .find(
                        argThat(
                                (Query q) ->
                                        q.getLimit() == 20
                                                && q.getQueryObject().containsKey("expiresAt")),
                        eq(SecurityAuditRun.class));
        assertThatThrownBy(() -> service.get(UUID.randomUUID().toString()))
                .isInstanceOfSatisfying(
                        AdminApiException.class,
                        e -> assertThat(e.status().value()).isEqualTo(404));
        when(mongo.find(any(Query.class), eq(SecurityAuditRun.class)))
                .thenThrow(new RuntimeException("SECRET_SENTINEL"));
        assertThatThrownBy(() -> service.list(1))
                .isInstanceOfSatisfying(
                        AdminApiException.class,
                        e -> {
                            assertThat(e.status().value()).isEqualTo(503);
                            assertThat(e.safeMessage()).doesNotContain("SECRET_SENTINEL");
                        });
        service.shutdown();
    }

    @Test
    void failedSaveReturns503AndNeverClearsExistingHistory() {
        when(mongo.save(any(SecurityAuditRun.class)))
                .thenThrow(new RuntimeException("SECRET_SENTINEL"));
        assertThatThrownBy(() -> service.execute(null))
                .isInstanceOfSatisfying(
                        AdminApiException.class,
                        e -> assertThat(e.status().value()).isEqualTo(503));
        verify(mongo, never()).remove(any(Query.class), eq(SecurityAuditRun.class));
        service.shutdown();
    }

    @Test
    void retentionIsRestrictedByCutoffAndIndexWithExtraFieldDoesNotPass() {
        var ops = mock(org.springframework.data.mongodb.core.index.IndexOperations.class);
        var index = mock(org.springframework.data.mongodb.core.index.IndexInfo.class);
        when(index.isUnique()).thenReturn(true);
        when(index.isIndexForFields(List.of("source", "url"))).thenReturn(true);
        when(index.getIndexFields())
                .thenReturn(
                        List.of(
                                org.springframework.data.mongodb.core.index.IndexField.create(
                                        "source",
                                        org.springframework.data.domain.Sort.Direction.ASC),
                                org.springframework.data.mongodb.core.index.IndexField.create(
                                        "url", org.springframework.data.domain.Sort.Direction.ASC),
                                org.springframework.data.mongodb.core.index.IndexField.create(
                                        "extra",
                                        org.springframework.data.domain.Sort.Direction.ASC)));
        when(ops.getIndexInfo()).thenReturn(List.of(index));
        when(mongo.indexOps("articles")).thenReturn(ops);
        var history =
                java.util.stream.IntStream.range(0, 20)
                        .mapToObj(
                                i ->
                                        new SecurityAuditRun(
                                                UUID.randomUUID().toString(),
                                                1,
                                                now.minusSeconds(i),
                                                now,
                                                0,
                                                new SecurityAuditRun.EnvironmentInfo(
                                                        "non-production", "unknown"),
                                                new SecurityAuditRun.Summary(0, 0, 0, 0),
                                                List.of(),
                                                now.plusSeconds(60)))
                        .toList();
        when(mongo.find(any(Query.class), eq(SecurityAuditRun.class))).thenReturn(history);
        var run = service.execute(null);
        assertThat(run.checks())
                .anyMatch(c -> c.id().equals("article-unique-index") && c.status().equals("fail"));
        verify(mongo)
                .remove(
                        argThat(
                                (Query q) ->
                                        q.getQueryObject().containsKey("_id")
                                                && q.getQueryObject().containsKey("startedAt")),
                        eq(SecurityAuditRun.class));
        service.shutdown();
    }
}
