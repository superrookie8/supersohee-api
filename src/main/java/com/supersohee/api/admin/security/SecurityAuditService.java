package com.supersohee.api.admin.security;

import com.supersohee.api.admin.error.AdminApiException;

import org.bson.Document;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
public class SecurityAuditService {
    private final MongoOperations mongo;
    private final Environment env;
    private final SecurityAuditProbe probe;
    private final Clock clock;
    private final AtomicBoolean running = new AtomicBoolean();
    private volatile Instant nextAllowed = Instant.MIN;
    // One bounded worker per process. A stuck driver call cannot grow thread count.
    private final ExecutorService worker =
            Executors.newSingleThreadExecutor(
                    r -> {
                        Thread t = new Thread(r, "security-audit");
                        t.setDaemon(true);
                        return t;
                    });

    @org.springframework.beans.factory.annotation.Autowired
    public SecurityAuditService(MongoOperations mongo, Environment env, SecurityAuditProbe probe) {
        this(mongo, env, probe, Clock.systemUTC());
    }

    SecurityAuditService(
            MongoOperations mongo, Environment env, SecurityAuditProbe probe, Clock clock) {
        this.mongo = mongo;
        this.env = env;
        this.probe = probe;
        this.clock = clock;
    }

    private final ExecutorService storageWorker =
            new ThreadPoolExecutor(
                    1,
                    1,
                    0,
                    TimeUnit.SECONDS,
                    new SynchronousQueue<>(),
                    r -> {
                        Thread t = new Thread(r, "audit-storage");
                        t.setDaemon(true);
                        return t;
                    });

    @jakarta.annotation.PreDestroy
    void shutdown() {
        worker.shutdownNow();
        storageWorker.shutdownNow();
    }

    private <T> T storage(java.util.concurrent.Callable<T> operation) {
        Future<T> pending = null;
        try {
            pending = storageWorker.submit(operation);
            return pending.get(5, TimeUnit.SECONDS);
        } catch (Exception ignored) {
            if (pending != null) pending.cancel(true);
            throw AdminApiException.unavailable("Security audit storage is unavailable.");
        }
    }

    public SecurityAuditRun execute(Authentication authentication) {
        if (!clock.instant().isAfter(nextAllowed) || !running.compareAndSet(false, true))
            throw AdminApiException.rateLimited();
        Instant started = clock.instant();
        nextAllowed = started.plusSeconds(30);
        var checks = new CopyOnWriteArrayList<SecurityAuditRun.Check>();
        boolean production = env.acceptsProfiles(Profiles.of("prod"));
        String origin = env.getProperty("app.frontend.url", "");
        var future =
                worker.submit(
                        () -> {
                            try {
                                collect(checks, authentication, production, origin);
                            } finally {
                                running.set(false);
                            }
                        });
        try {
            future.get(15, TimeUnit.SECONDS);
        } catch (Exception ignored) {
            // Do not clear running while an uninterruptible driver is still executing.
            add(
                    checks,
                    "run-time-limit",
                    "deployment",
                    "Execution time limit",
                    "unknown",
                    "manual-review",
                    "The bounded execution did not complete; partial findings are retained.",
                    "Check service availability before retrying.");
        }
        Instant finished = clock.instant();
        List<SecurityAuditRun.Check> snapshot = List.copyOf(checks);
        var summary =
                new SecurityAuditRun.Summary(
                        count(snapshot, "pass"),
                        count(snapshot, "warn"),
                        count(snapshot, "fail"),
                        count(snapshot, "unknown"));
        var run =
                new SecurityAuditRun(
                        UUID.randomUUID().toString(),
                        1,
                        started,
                        finished,
                        Math.max(0, Duration.between(started, finished).toMillis()),
                        new SecurityAuditRun.EnvironmentInfo(
                                production ? "production" : "non-production",
                                probe.targetKind(origin, production)),
                        summary,
                        snapshot,
                        started.plus(Duration.ofDays(30)));
        return storage(
                () -> {
                    mongo.save(run);
                    mongo.remove(
                            Query.query(Criteria.where("expiresAt").lte(clock.instant())),
                            SecurityAuditRun.class);
                    var latest =
                            mongo.find(
                                    new Query()
                                            .with(Sort.by(Sort.Direction.DESC, "startedAt"))
                                            .limit(20),
                                    SecurityAuditRun.class);
                    if (latest.size() == 20)
                        mongo.remove(
                                Query.query(
                                        Criteria.where("_id")
                                                .nin(
                                                        latest.stream()
                                                                .map(SecurityAuditRun::id)
                                                                .toList())
                                                .and("startedAt")
                                                .lte(latest.get(19).startedAt())),
                                SecurityAuditRun.class);
                    return run;
                });
    }

    public List<SecurityAuditRun> list(int limit) {
        if (limit < 1 || limit > 20)
            throw AdminApiException.badRequest("limit must be between 1 and 20.");
        return storage(
                () ->
                        mongo.find(
                                Query.query(Criteria.where("expiresAt").gt(clock.instant()))
                                        .with(Sort.by(Sort.Direction.DESC, "startedAt"))
                                        .limit(limit),
                                SecurityAuditRun.class));
    }

    public SecurityAuditRun get(String id) {
        try {
            UUID.fromString(id);
        } catch (RuntimeException ignored) {
            throw AdminApiException.notFound("Security audit run");
        }
        SecurityAuditRun run =
                storage(
                        () ->
                                mongo.findOne(
                                        Query.query(
                                                Criteria.where("_id")
                                                        .is(id)
                                                        .and("expiresAt")
                                                        .gt(clock.instant())),
                                        SecurityAuditRun.class));
        if (run == null) throw AdminApiException.notFound("Security audit run");
        return run;
    }

    private void collect(
            List<SecurityAuditRun.Check> checks, Authentication auth, boolean prod, String origin) {
        boolean admin =
                auth != null
                        && auth.getAuthorities().stream()
                                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        add(
                checks,
                "current-admin",
                "authentication",
                "Current administrator authorization",
                admin ? "pass" : "fail",
                "configuration",
                "Observed the current request authorization after the database role guard.",
                "Use an authorized member session.");
        add(
                checks,
                "production-profile",
                "configuration",
                "Backend profile",
                prod ? "pass" : "warn",
                "configuration",
                "Checked whether the production profile is active.",
                "Use the production profile for the deployed service.");
        add(
                checks,
                "frontend-origin",
                "configuration",
                "Frontend origin and CORS",
                probe.targetKind(origin, prod).equals("https-configured") ? "pass" : "warn",
                "configuration",
                "Checked the configured single frontend CORS origin scheme; no address is"
                    + " retained.",
                "Configure the exact public HTTPS frontend origin.");
        boolean seed = env.getProperty("app.seed.enabled", Boolean.class, false);
        boolean indexes =
                env.getProperty("app.article-index.enabled", Boolean.class, true)
                        || env.getProperty("app.user-index.enabled", Boolean.class, true)
                        || env.getProperty(
                                "spring.data.mongodb.auto-index-creation", Boolean.class, false);
        add(
                checks,
                "startup-writes",
                "configuration",
                "Automatic initialization",
                seed || indexes ? "warn" : "pass",
                "configuration",
                "Checked configured seed and automatic index creation flags.",
                "Review initialization changes separately from application deployment.");
        key(
                checks,
                "jwt-key",
                "authentication",
                "JWT signing key",
                env.getProperty("jwt.secret", ""));
        key(
                checks,
                "crawler-key",
                "crawler",
                "Dedicated article import key",
                env.getProperty(
                        "article.import.key",
                        env.getProperty("SUPERSOHEE_ARTICLE_IMPORT_KEY", "")));
        key(
                checks,
                "exchange-key",
                "authentication",
                "Provider exchange key",
                env.getProperty(
                        "google.auth.exchange-key", env.getProperty("AUTH_EXCHANGE_KEY", "")));
        db(
                checks,
                "database-connectivity",
                "Database connectivity",
                () -> {
                    mongo.executeCommand(new Document("ping", 1));
                    return true;
                },
                "A read-only database ping completed.",
                "Check database availability.");
        db(
                checks,
                "administrator-count",
                "Exclusive administrator assignment",
                () ->
                        mongo.count(
                                        Query.query(Criteria.where("role").is("ADMIN"))
                                                .maxTime(Duration.ofSeconds(2)),
                                        "users")
                                == 1,
                "Checked whether exactly one database member has the administrator role; no"
                    + " identifiers are retained.",
                "Review administrator assignments through the approved operator process.");
        db(
                checks,
                "article-unique-index",
                "Article identity index",
                () ->
                        mongo.indexOps("articles").getIndexInfo().stream()
                                .anyMatch(
                                        i ->
                                                i.isUnique()
                                                        && i.getIndexFields().size() == 2
                                                        && i.isIndexForFields(
                                                                List.of("source", "url"))),
                "Read existing index metadata for a unique article source and URL identity.",
                "Review the existing article index before importing; this check never creates it.");
        checks.addAll(probe.inspect(origin, prod));
        add(
                checks,
                "upload-content-security",
                "upload",
                "Malicious upload content review",
                "unknown",
                "manual-review",
                "No file upload or malware scan was performed.",
                "Review file signature validation and scan suspicious uploads in an isolated"
                    + " environment.");
        add(
                checks,
                "import-policy",
                "crawler",
                "Article import boundary review",
                "unknown",
                "manual-review",
                "No article import mutation was sent.",
                "Review import-key rejection and URL validation fixture results before enabling"
                    + " ingestion.");
        add(
                checks,
                "external-rate-limit",
                "deployment",
                "Ingress rate limiting",
                "unknown",
                "manual-review",
                "External ingress rate limits were not inspected.",
                "Confirm login and exchange rate limits in the existing ingress configuration.");
        add(
                checks,
                "secret-rotation",
                "configuration",
                "Secret rotation review",
                "unknown",
                "manual-review",
                "Key age, reuse and revocation were not inspected.",
                "Check rotation records and revoke obsolete keys through the approved secret"
                    + " store.");
        add(
                checks,
                "storage-access",
                "upload",
                "Upload storage access controls",
                "unknown",
                "manual-review",
                "Object storage ACLs and public exposure were not inspected.",
                "Review bucket access and signed URL expiry in the configured storage account.");
        add(
                checks,
                "backend-anonymous-boundary",
                "authentication",
                "Backend anonymous access boundary",
                "unknown",
                "manual-review",
                "No trusted self origin is configured; no backend HTTP target was invented.",
                "Verify anonymous 401 and ordinary-member 403 responses on the deployed"
                    + " administrator endpoints.");
    }

    private void key(
            List<SecurityAuditRun.Check> c,
            String id,
            String category,
            String title,
            String value) {
        add(
                c,
                id,
                category,
                title,
                value != null
                                && value.getBytes(java.nio.charset.StandardCharsets.UTF_8).length
                                        >= 32
                        ? "pass"
                        : "fail",
                "configuration",
                "Checked configured key presence and minimum byte length only; entropy and rotation"
                    + " are not measured.",
                "Configure a strong dedicated secret through the approved secret store.");
    }

    private void db(
            List<SecurityAuditRun.Check> c,
            String id,
            String title,
            java.util.function.BooleanSupplier action,
            String evidence,
            String remediation) {
        long begin = System.nanoTime();
        String status;
        try {
            status = action.getAsBoolean() ? "pass" : "fail";
        } catch (RuntimeException ignored) {
            status = "unknown";
            evidence =
                    "The read-only database check could not complete; no exception details are"
                        + " retained.";
        }
        c.add(
                new SecurityAuditRun.Check(
                        id,
                        "data",
                        title,
                        status,
                        "database-read",
                        evidence,
                        remediation,
                        clock.instant(),
                        (System.nanoTime() - begin) / 1_000_000,
                        null));
    }

    private void add(
            List<SecurityAuditRun.Check> c,
            String id,
            String category,
            String title,
            String status,
            String type,
            String evidence,
            String remediation) {
        c.add(
                new SecurityAuditRun.Check(
                        id,
                        category,
                        title,
                        status,
                        type,
                        evidence,
                        remediation,
                        clock.instant(),
                        0,
                        null));
    }

    private long count(List<SecurityAuditRun.Check> c, String status) {
        return c.stream().filter(x -> x.status().equals(status)).count();
    }
}
