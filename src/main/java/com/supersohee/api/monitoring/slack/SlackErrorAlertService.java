package com.supersohee.api.monitoring.slack;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.servlet.HandlerMapping;

import java.net.URI;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;

public class SlackErrorAlertService {

    private static final Logger log = LoggerFactory.getLogger(SlackErrorAlertService.class);
    private static final String SERVICE_NAME = "supersohee-api";
    private static final Duration RATE_WINDOW = Duration.ofMinutes(1);
    private static final int MAX_CAUSE_DEPTH = 16;

    private final SlackAlertProperties properties;
    private final SlackWebhookClient webhookClient;
    private final Executor executor;
    private final Clock clock;
    private final URI webhookUri;
    private final Object stateLock = new Object();
    private final LinkedHashMap<String, DedupeEntry> dedupeEntries = new LinkedHashMap<>(16, 0.75f, true);
    private final Deque<Instant> acceptedSendTimes = new ArrayDeque<>();

    SlackErrorAlertService(
            SlackAlertProperties properties,
            SlackWebhookClient webhookClient,
            Executor executor,
            Clock clock) {
        this.properties = properties;
        this.webhookClient = webhookClient;
        this.executor = executor;
        this.clock = clock;
        this.webhookUri = resolveWebhookUri(properties);
    }

    public void report(HttpServletRequest request, int status, Throwable failure) {
        if (!isOperational() || status < 500) {
            return;
        }
        Object routeAttribute = request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);
        String routePattern = routeAttribute == null ? "unmatched" : routeAttribute.toString();
        report(request.getMethod(), routePattern, status, failure);
    }

    void report(String method, String routePattern, int status, Throwable failure) {
        if (!isOperational() || status < 500) {
            return;
        }

        AlertDetails details = alertDetails(method, routePattern, status, failure);
        PreparedAlert prepared = prepare(details);
        if (prepared == null) {
            return;
        }

        try {
            executor.execute(() -> webhookClient.send(webhookUri, format(prepared)));
        } catch (RejectedExecutionException failureToSchedule) {
            log.warn("Slack error alert queue is full; alert was dropped");
        } catch (RuntimeException failureToSchedule) {
            log.warn("Slack error alert could not be scheduled ({})",
                    failureToSchedule.getClass().getSimpleName());
        }
    }

    private PreparedAlert prepare(AlertDetails details) {
        Instant now = clock.instant();
        String fingerprint = details.fingerprint();
        synchronized (stateLock) {
            Duration dedupeWindow = positiveDuration(properties.getDedupeWindow(), Duration.ofMinutes(5));
            DedupeEntry previous = dedupeEntries.get(fingerprint);
            if (previous != null && now.isBefore(previous.acceptedAt().plus(dedupeWindow))) {
                previous.incrementSuppressed();
                return null;
            }

            Instant rateCutoff = now.minus(RATE_WINDOW);
            while (!acceptedSendTimes.isEmpty() && !acceptedSendTimes.peekFirst().isAfter(rateCutoff)) {
                acceptedSendTimes.removeFirst();
            }
            if (acceptedSendTimes.size() >= Math.max(1, properties.getMaxPerMinute())) {
                return null;
            }

            int suppressed = previous == null ? 0 : previous.suppressed();
            dedupeEntries.put(fingerprint, new DedupeEntry(now));
            while (dedupeEntries.size() > Math.max(1, properties.getCacheSize())) {
                String eldest = dedupeEntries.keySet().iterator().next();
                dedupeEntries.remove(eldest);
            }
            acceptedSendTimes.addLast(now);
            return new PreparedAlert(details, now, UUID.randomUUID().toString(), suppressed);
        }
    }

    private AlertDetails alertDetails(String method, String routePattern, int status, Throwable failure) {
        Throwable root = rootCause(failure);
        String exceptionType = root == null ? "handled-5xx" : root.getClass().getName();
        String topFrame = root == null ? "not-available" : topApplicationFrame(root);
        return new AlertDetails(
                safeValue(method, 16, "UNKNOWN"),
                safeValue(routePattern, 200, "unmatched"),
                status,
                safeValue(exceptionType, 200, "unknown"),
                safeValue(topFrame, 240, "outside-application"));
    }

    private Throwable rootCause(Throwable failure) {
        Throwable current = failure;
        int depth = 0;
        while (current != null && current.getCause() != null && current.getCause() != current
                && depth++ < MAX_CAUSE_DEPTH) {
            current = current.getCause();
        }
        return current;
    }

    private String topApplicationFrame(Throwable failure) {
        for (StackTraceElement frame : failure.getStackTrace()) {
            if (frame.getClassName().startsWith("com.supersohee.api.")) {
                return frame.getClassName() + "#" + frame.getMethodName() + ":" + frame.getLineNumber();
            }
        }
        return "outside-application";
    }

    private String format(PreparedAlert alert) {
        AlertDetails details = alert.details();
        StringBuilder text = new StringBuilder(384)
                .append(":rotating_light: Supersohee backend error\n")
                .append("service=").append(SERVICE_NAME).append('\n')
                .append("environment=").append(safeValue(properties.getEnvironment(), 80, "unknown")).append('\n')
                .append("occurredAt=").append(alert.occurredAt()).append('\n')
                .append("status=").append(details.status()).append('\n')
                .append("request=").append(details.method()).append(' ').append(details.routePattern()).append('\n')
                .append("exception=").append(details.exceptionType()).append('\n')
                .append("frame=").append(details.topFrame()).append('\n')
                .append("alertId=").append(alert.alertId());
        if (alert.suppressedDuplicates() > 0) {
            text.append('\n').append("suppressedDuplicates=").append(alert.suppressedDuplicates());
        }
        return text.toString();
    }

    private String safeValue(String value, int maxLength, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        String safe = value.replace('\r', ' ').replace('\n', ' ').replace('\t', ' ')
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
        return safe.length() <= maxLength ? safe : safe.substring(0, maxLength);
    }

    private URI resolveWebhookUri(SlackAlertProperties configured) {
        if (!configured.isEnabled() || configured.getWebhookUrl() == null || configured.getWebhookUrl().isBlank()) {
            return null;
        }
        try {
            URI candidate = URI.create(configured.getWebhookUrl().trim());
            String host = candidate.getHost();
            boolean slackHost = "hooks.slack.com".equalsIgnoreCase(host)
                    || "hooks.slack-gov.com".equalsIgnoreCase(host);
            if (!"https".equalsIgnoreCase(candidate.getScheme()) || !slackHost) {
                log.warn("Slack error alerts are disabled because the webhook URI is not an approved HTTPS Slack host");
                return null;
            }
            return candidate;
        } catch (IllegalArgumentException invalidUri) {
            log.warn("Slack error alerts are disabled because the webhook URI is invalid");
            return null;
        }
    }

    private boolean isOperational() {
        return properties.isEnabled() && webhookUri != null;
    }

    private Duration positiveDuration(Duration configured, Duration fallback) {
        return configured == null || configured.isZero() || configured.isNegative() ? fallback : configured;
    }

    private record AlertDetails(
            String method,
            String routePattern,
            int status,
            String exceptionType,
            String topFrame) {

        String fingerprint() {
            return status + "|" + method + "|" + routePattern + "|" + exceptionType + "|" + topFrame;
        }
    }

    private record PreparedAlert(
            AlertDetails details,
            Instant occurredAt,
            String alertId,
            int suppressedDuplicates) {
    }

    private static final class DedupeEntry {
        private final Instant acceptedAt;
        private int suppressed;

        private DedupeEntry(Instant acceptedAt) {
            this.acceptedAt = acceptedAt;
        }

        private Instant acceptedAt() {
            return acceptedAt;
        }

        private int suppressed() {
            return suppressed;
        }

        private void incrementSuppressed() {
            suppressed++;
        }
    }
}
