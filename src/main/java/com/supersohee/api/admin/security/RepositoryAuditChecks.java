package com.supersohee.api.admin.security;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import java.time.Instant;
import java.util.List;
import java.util.function.Consumer;

@Component
public class RepositoryAuditChecks {
    static final List<String> IDS = List.of("repository-web-actions", "repository-api-actions",
            "repository-web-alerts", "repository-api-alerts", "crawler-workflow");
    private final RepositoryAuditHttp http;
    private final Environment env;
    public RepositoryAuditChecks(RepositoryAuditHttp http, Environment env) { this.http = http; this.env = env; }
    public void inspect(Consumer<SecurityAuditRun.Check> sink) {
        String token = env.getProperty("GITHUB_SECURITY_TOKEN", "");
        for (String kind : List.of("web", "api")) {
            if (Thread.currentThread().isInterrupted()) return;
            String repo = kind.equals("web") ? "sofanpage" : "supersohee-api";
            String base = "/repos/superrookie8/" + repo;
            String id = "repository-" + kind + "-actions";
            try {
                var reply = http.get(base + "/actions/runs?per_page=1", token);
                sink.accept(actions(id, reply));
            } catch (Exception ignored) { sink.accept(unknown(id)); }
            id = "repository-" + kind + "-alerts";
            if (token.isBlank()) { sink.accept(result(id, "unknown", "Optional server token is absent; alerts were not queried.", null)); continue; }
            try { sink.accept(alerts(id, http.get(base + "/dependabot/alerts?state=open&per_page=100", token))); }
            catch (Exception ignored) { sink.accept(unknown(id)); }
        }
        if (Thread.currentThread().isInterrupted()) return;
        try {
            String path = "/repos/superrookie8/sofanpage/actions/workflows/supersohee-weekly-crawler.yml";
            var workflow = http.get(path, token);
            if (workflow.status() != 200 || workflow.body() == null || !workflow.body().path("state").isTextual()) {
                sink.accept(unknown("crawler-workflow")); return;
            }
            boolean enabled = workflow.body().path("state").asText().equals("active");
            var reply = http.get(path + "/runs?event=schedule&per_page=1", token);
            var scheduled = scheduled(reply, Instant.now());
            sink.accept(result("crawler-workflow", enabled ? scheduled.status() : "warn",
                    "Workflow enabled=" + enabled + ". Latest scheduled execution only: " + scheduled.evidence()
                    + " Latest repository-wide execution is reported separately. Workflow revision does not identify the checked-out crawler branch. No import receipt was inspected.", scheduled.observedStatus()));
        } catch (Exception ignored) { sink.accept(unknown("crawler-workflow")); }
    }
    static SecurityAuditRun.Check actions(String id, RepositoryAuditHttp.Reply r) {
        if (r.status() != 200 || r.body() == null || !r.body().path("workflow_runs").isArray()) return unknown(id);
        JsonNode runs = r.body().get("workflow_runs");
        if (runs.isEmpty()) return result(id, "unknown", "No execution was returned; absence is not a successful check.", 200);
        JsonNode run = runs.get(0);
        String state = run.path("status").asText();
        String conclusion = run.path("conclusion").asText();
        String status;
        String observed;
        if (state.equals("completed") && conclusion.equals("success")) { status = "pass"; observed = "success"; }
        else if (state.equals("completed") && List.of("failure", "timed_out", "action_required", "startup_failure").contains(conclusion)) { status = "fail"; observed = "failed"; }
        else if (List.of("queued", "in_progress", "waiting", "pending", "requested").contains(state)) { status = "warn"; observed = "pending"; }
        else if (state.equals("completed") && List.of("cancelled", "skipped", "neutral", "stale").contains(conclusion)) { status = "warn"; observed = "not successful"; }
        else return unknown(id);
        String timestamp = "unavailable";
        try { timestamp = Instant.parse(run.path("created_at").asText()).toString(); } catch (RuntimeException ignored) {}
        return result(id, status, "Observed one execution: " + observed + "; createdAt=" + timestamp
                + ". This does not certify all tests, deployment, or database imports.", 200);
    }
    static SecurityAuditRun.Check scheduled(RepositoryAuditHttp.Reply reply, Instant now) {
        var observed = actions("crawler-workflow", reply);
        if (observed.status().equals("unknown")) return observed;
        try {
            Instant created = Instant.parse(reply.body().path("workflow_runs").get(0).path("created_at").asText());
            if (created.isAfter(now)) return unknown("crawler-workflow");
            if (created.isBefore(now.minus(java.time.Duration.ofDays(8))))
                return result("crawler-workflow", "warn", "The latest scheduled run is older than eight days. " + observed.evidence(), 200);
            return observed;
        } catch (RuntimeException ignored) { return unknown("crawler-workflow"); }
    }
    static SecurityAuditRun.Check alerts(String id, RepositoryAuditHttp.Reply r) {
        if (r.status() != 200 || r.body() == null || !r.body().isArray() || r.body().size() > 100) return unknown(id);
        int critical = 0, high = 0, moderate = 0, low = 0, unclassified = 0;
        for (JsonNode alert : r.body()) {
            if (!alert.path("state").asText().equals("open")) return unknown(id);
            switch (alert.path("security_advisory").path("severity").asText()) {
                case "critical" -> critical++;
                case "high" -> high++;
                case "moderate" -> moderate++;
                case "low" -> low++;
                default -> unclassified++;
            }
        }
        String status = critical + high > 0 ? "fail" : r.body().size() > 0 || r.more() ? "warn" : "pass";
        return result(id, status, "Open Dependabot alerts, first page only (limit 100): critical=" + critical
                + ", high=" + high + ", moderate=" + moderate + ", low=" + low + ", unclassified=" + unclassified
                + "; morePages=" + r.more() + ". Code scanning and secret scanning were not inspected.", 200);
    }
    static SecurityAuditRun.Check unknown(String id) {
        return result(id, "unknown", "The bounded provider read was unavailable, unauthorized, rate-limited, or invalid; no response details retained.", null);
    }
    static SecurityAuditRun.Check result(String id, String status, String evidence, Integer code) {
        return new SecurityAuditRun.Check(id, id.equals("crawler-workflow") ? "crawler" : "repository", id,
                status, "provider-api", evidence, "Review the fixed repository workflow or read permission; do not infer data ingestion from Actions success.", Instant.now(), 0, code);
    }
}
