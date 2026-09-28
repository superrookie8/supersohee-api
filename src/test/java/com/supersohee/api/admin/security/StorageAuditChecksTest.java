package com.supersohee.api.admin.security;

import org.bson.Document;
import org.junit.jupiter.api.Test;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.mock.env.MockEnvironment;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

class StorageAuditChecksTest {
    final MongoOperations mongo = mock(MongoOperations.class);
    final R2AuditCheck r2 = mock(R2AuditCheck.class);
    Document response(List<Document> docs) { return new Document("cursor", new Document("id", 0L).append("firstBatch", docs)); }
    List<SecurityAuditRun.Check> inspect() {
        when(r2.inspect()).thenReturn(StorageAuditChecks.unknown("storage-r2"));
        var checks = new ArrayList<SecurityAuditRun.Check>();
        new StorageAuditChecks(mongo, r2).inspect(checks::add); return checks;
    }
    @Test void unavailableAndMissingCollectionsAreUnknownWithoutMutations() {
        when(mongo.executeCommand(any(Document.class))).thenThrow(new IllegalStateException("SECRET_SENTINEL"));
        var checks = inspect();
        assertThat(checks).hasSize(4).allMatch(c -> c.status().equals("unknown"));
        assertThat(checks.toString()).doesNotContain("SECRET_SENTINEL");
        when(mongo.executeCommand(any(Document.class))).thenReturn(response(List.of()));
        assertThat(inspect()).allMatch(c -> c.status().equals("unknown"));
        verify(mongo, never()).save(any());
    }
    @Test void everyReadIsBoundedAndSampleIsNotWholeCollectionFreshness() {
        when(mongo.executeCommand(any(Document.class))).thenAnswer(invocation -> {
            Document command = invocation.getArgument(0);
            assertThat(command.getInteger("maxTimeMS")).isEqualTo(1000);
            if (command.containsKey("listCollections")) return response(List.of("articles", "schedules", "admin_photo.files", "user_photo.files").stream().map(s -> new Document("name", s)).toList());
            assertThat(command.getBoolean("allowDiskUse")).isFalse();
            assertThat(command.getList("pipeline", Document.class).get(0).getInteger("$limit")).isEqualTo(1001);
            assertThat(command.toString()).doesNotContain("$sort", "$out", "$merge");
            return response(List.of(new Document("_id", "jumpball").append("count", 1001).append("bytes", 100).append("latest", new Date(0))));
        });
        var checks = inspect();
        assertThat(checks).anyMatch(c -> c.id().equals("storage-mongo") && c.status().equals("warn"));
        assertThat(checks).anyMatch(c -> c.id().equals("crawler-data-freshness") && c.status().equals("unknown") && c.evidence().contains("WITHIN THE SAMPLE"));
        assertThat(checks).anyMatch(c -> c.id().equals("storage-gridfs") && c.evidence().contains("declaredSampleBytes=100"));
    }
    @Test void r2HeadSuccessFailureAndInvalidConfigurationNeverExposeSecrets() {
        var head = mock(R2AuditCheck.Head.class);
        var env = new MockEnvironment().withProperty("cloudflare.r2.endpoint", "https://account.r2.cloudflarestorage.com")
                .withProperty("cloudflare.r2.access-key-id", "SECRET_SENTINEL")
                .withProperty("cloudflare.r2.secret-access-key", "SECRET_SENTINEL")
                .withProperty("cloudflare.r2.bucket-name", "PRIVATE_BUCKET");
        var check = new R2AuditCheck(env, head);
        assertThat(check.inspect().status()).isEqualTo("pass");
        assertThat(check.inspect().toString()).doesNotContain("SECRET_SENTINEL", "PRIVATE_BUCKET", "account.r2");
        doThrow(new IllegalStateException("SECRET_SENTINEL")).when(head).read(any(), any(), any(), any());
        assertThat(check.inspect().status()).isEqualTo("unknown");
        env.setProperty("cloudflare.r2.endpoint", "http://127.0.0.1/");
        clearInvocations(head);
        assertThat(check.inspect().status()).isEqualTo("unknown");
        verifyNoInteractions(head);
    }
}
