package com.supersohee.api.admin.security;

import org.bson.Document;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.stereotype.Component;
import java.time.Instant;
import java.util.*;
import java.util.function.Consumer;

/** Bounded metadata samples; never reads image bytes or returns document identifiers. */
@Component
public class StorageAuditChecks {
    static final List<String> IDS = List.of("storage-mongo", "crawler-data-freshness", "storage-gridfs", "storage-r2");
    static final int LIMIT = 1001;
    private final MongoOperations mongo;
    private final R2AuditCheck r2;
    public StorageAuditChecks(MongoOperations mongo, R2AuditCheck r2) { this.mongo = mongo; this.r2 = r2; }
    public void inspect(Consumer<SecurityAuditRun.Check> sink) {
        Set<String> collections;
        try {
            var response = mongo.executeCommand(new Document("listCollections", 1)
                    .append("filter", new Document("name", new Document("$in", List.of("articles", "schedules", "admin_photo.files", "user_photo.files"))))
                    .append("nameOnly", true).append("cursor", new Document("batchSize", 10)).append("maxTimeMS", 1000));
            collections = new HashSet<>();
            for (Document d : batch(response)) collections.add(d.getString("name"));
        } catch (RuntimeException ignored) {
            collections = Set.of();
        }
        StringBuilder mongoEvidence = new StringBuilder("Unsorted metadata samples, at most 1001 documents per collection; not total database size. ");
        boolean mongoOk = true, truncated = false;
        for (String name : List.of("articles", "schedules")) {
            if (Thread.currentThread().isInterrupted()) return;
            try {
                if (!collections.contains(name)) throw new IllegalStateException();
                var rows = aggregate(name, new Document("$group", new Document("_id", null)
                        .append("count", new Document("$sum", 1)).append("bytes", new Document("$sum", new Document("$bsonSize", "$$ROOT")))));
                long count = number(rows, "count"), bytes = number(rows, "bytes");
                mongoEvidence.append(name).append(": sampled=").append(count).append(", sampledBsonBytes=").append(bytes).append(". ");
                truncated |= count >= LIMIT;
            } catch (RuntimeException ignored) { mongoOk = false; }
        }
        sink.accept(mongoOk ? result("storage-mongo", truncated ? "warn" : "pass", mongoEvidence + "Sample cap reached=" + truncated + ". No backup or restore was tested.") : unknown("storage-mongo"));
        if (Thread.currentThread().isInterrupted()) return;
        try {
            if (!collections.contains("articles")) throw new IllegalStateException();
            var rows = aggregate("articles", new Document("$group", new Document("_id", "$source")
                    .append("count", new Document("$sum", 1)).append("latest", new Document("$max", "$crawledAt"))));
            StringBuilder evidence = new StringBuilder("Unsorted sample of at most 1001 articles. Counts and most recent timestamps below are WITHIN THE SAMPLE, not whole-collection latest. ");
            long total = 0, invalid = 0;
            Set<String> sources = Set.of("jumpball", "rookie", "other", "manual");
            for (Document row : rows) {
                long count = positiveNumber(row.get("count")); total += count;
                if (!(row.get("_id") instanceof String source) || !sources.contains(source)) { invalid += count; continue; }
                String latest = row.get("latest") instanceof Date date ? date.toInstant().toString() : "unavailable";
                if (latest.equals("unavailable")) invalid += count;
                evidence.append(source).append(": sampled=").append(count).append(", sampleLatest=").append(latest).append(". ");
            }
            evidence.append("Sample cap reached=").append(total >= LIMIT).append("; unclassified or missing sample date count=").append(invalid)
                    .append(". On-demand sources have no scheduled freshness requirement. Whole-collection freshness remains UNKNOWN: no indexed latest query was performed. No per-run created/existing receipt or complete search coverage was verified.");
            sink.accept(result("crawler-data-freshness", "unknown", evidence.toString()));
        } catch (RuntimeException ignored) { sink.accept(unknown("crawler-data-freshness")); }
        if (Thread.currentThread().isInterrupted()) return;
        try {
            StringBuilder evidence = new StringBuilder("GridFS file metadata only; at most 1001 files per bucket. ");
            boolean capped = false;
            for (String name : List.of("admin_photo.files", "user_photo.files")) {
                if (!collections.contains(name)) throw new IllegalStateException();
                var rows = aggregate(name, new Document("$group", new Document("_id", null)
                        .append("count", new Document("$sum", 1)).append("bytes", new Document("$sum", "$length"))));
                long count = number(rows, "count"); capped |= count >= LIMIT;
                evidence.append(name.equals("admin_photo.files") ? "administrator" : "member")
                        .append(": sampledFiles=").append(count).append(", declaredSampleBytes=").append(number(rows, "bytes")).append(". ");
            }
            evidence.append("Sample cap reached=").append(capped).append(". Chunk integrity, image contents, public access, and restoration were not inspected.");
            sink.accept(result("storage-gridfs", capped ? "warn" : "pass", evidence.toString()));
        } catch (RuntimeException ignored) { sink.accept(unknown("storage-gridfs")); }
        if (!Thread.currentThread().isInterrupted()) sink.accept(r2.inspect());
    }
    private List<Document> aggregate(String collection, Document group) {
        return batch(mongo.executeCommand(new Document("aggregate", collection)
                .append("pipeline", List.of(new Document("$limit", LIMIT), group))
                .append("cursor", new Document("batchSize", LIMIT)).append("maxTimeMS", 1000).append("allowDiskUse", false)));
    }
    private static List<Document> batch(Document response) {
        if (response == null || !(response.get("cursor") instanceof Document cursor)
                || !(cursor.get("firstBatch") instanceof List<?> rows) || rows.size() > LIMIT
                || !(cursor.get("id") instanceof Number id) || id.longValue() != 0) throw new IllegalStateException();
        List<Document> docs = new ArrayList<>();
        for (Object row : rows) { if (!(row instanceof Document doc)) throw new IllegalStateException(); docs.add(doc); }
        return docs;
    }
    private static long number(List<Document> rows, String key) { return rows.isEmpty() ? 0 : positiveNumber(rows.get(0).get(key)); }
    private static long positiveNumber(Object value) {
        if (!(value instanceof Number n) || n.longValue() < 0) throw new IllegalStateException();
        return n.longValue();
    }
    static SecurityAuditRun.Check unknown(String id) {
        return result(id, "unknown", "Bounded metadata read was unavailable, unauthorized, incomplete, or the expected collection was absent; no exception details retained.");
    }
    static SecurityAuditRun.Check result(String id, String status, String evidence) {
        return new SecurityAuditRun.Check(id, id.equals("crawler-data-freshness") ? "crawler" : "storage", id,
                status, "database-read", evidence, "Review read access and the stated sample scope; this check does not modify data, indexes, files, or backups.", Instant.now(), 0, null);
    }
}
