package com.supersohee.api.auth.google;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Process-local MVP protection only. These bounded TTL maps and the fixed-window
 * limiter are neither distributed nor durable; production horizontal scaling
 * must replace them with a shared atomic store such as Redis.
 */
@Component
public class GoogleExchangeProtection {

    private static final int MAXIMUM_IDEMPOTENCY_KEY_LENGTH = 128;
    private static final int MAXIMUM_ID_TOKEN_LENGTH = 16_384;

    private final byte[] exchangeKey;
    private final Duration ttl;
    private final int maximumEntries;
    private final int rateLimitRequests;
    private final Duration rateLimitWindow;
    private final Clock clock;
    private final Map<String, IdempotencyEntry> idempotencyEntries = new LinkedHashMap<>();
    private final Map<String, ReplayEntry> replayEntries = new LinkedHashMap<>();
    private final ArrayDeque<Instant> requestTimes = new ArrayDeque<>();

    @Autowired
    public GoogleExchangeProtection(
            @Value("${google.auth.exchange-key:${AUTH_EXCHANGE_KEY:}}") String exchangeKey,
            @Value("${google.auth.idempotency-ttl-seconds:300}") long ttlSeconds,
            @Value("${google.auth.maximum-cache-entries:10000}") int maximumEntries,
            @Value("${google.auth.rate-limit.requests:60}") int rateLimitRequests,
            @Value("${google.auth.rate-limit.window-seconds:60}") long rateLimitWindowSeconds) {
        this(
                exchangeKey,
                Duration.ofSeconds(ttlSeconds),
                maximumEntries,
                rateLimitRequests,
                Duration.ofSeconds(rateLimitWindowSeconds),
                Clock.systemUTC());
    }

    GoogleExchangeProtection(
            String exchangeKey,
            Duration ttl,
            int maximumEntries,
            int rateLimitRequests,
            Duration rateLimitWindow,
            Clock clock) {
        if (exchangeKey == null || exchangeKey.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException("google.auth.exchange-key must be at least 32 bytes");
        }
        if (ttl.isZero() || ttl.isNegative()
                || maximumEntries < 1
                || rateLimitRequests < 1
                || rateLimitWindow.isZero()
                || rateLimitWindow.isNegative()) {
            throw new IllegalStateException("Google exchange protection limits must be positive");
        }
        this.exchangeKey = exchangeKey.getBytes(StandardCharsets.UTF_8);
        this.ttl = ttl;
        this.maximumEntries = maximumEntries;
        this.rateLimitRequests = rateLimitRequests;
        this.rateLimitWindow = rateLimitWindow;
        this.clock = clock;
    }

    public GoogleExchangeResponse execute(
            String presentedExchangeKey,
            String idempotencyKey,
            String idToken,
            Supplier<GoogleExchangeResponse> exchange) {
        authenticate(presentedExchangeKey);
        validateRequest(idempotencyKey, idToken);

        String idempotencyHash = fingerprint(idempotencyKey);
        String tokenHash = fingerprint(idToken);
        GoogleExchangeResponse cached = begin(idempotencyHash, tokenHash);
        if (cached != null) {
            return cached;
        }

        try {
            GoogleExchangeResponse response = exchange.get();
            complete(idempotencyHash, tokenHash, response);
            return response;
        } catch (RuntimeException e) {
            abort(idempotencyHash, tokenHash);
            throw e;
        }
    }

    private void authenticate(String presentedExchangeKey) {
        byte[] presented = presentedExchangeKey == null
                ? new byte[0]
                : presentedExchangeKey.getBytes(StandardCharsets.UTF_8);
        if (!MessageDigest.isEqual(exchangeKey, presented)) {
            throw GoogleExchangeException.unauthorized();
        }
    }

    private void validateRequest(String idempotencyKey, String idToken) {
        if (idempotencyKey == null || idempotencyKey.isBlank()
                || idempotencyKey.length() > MAXIMUM_IDEMPOTENCY_KEY_LENGTH
                || idToken == null || idToken.isBlank()
                || idToken.length() > MAXIMUM_ID_TOKEN_LENGTH) {
            throw GoogleExchangeException.invalidRequest();
        }
    }

    private synchronized GoogleExchangeResponse begin(String idempotencyHash, String tokenHash) {
        Instant now = clock.instant();
        cleanup(now);

        IdempotencyEntry existing = idempotencyEntries.get(idempotencyHash);
        if (existing != null) {
            if (!existing.tokenHash().equals(tokenHash)) {
                throw GoogleExchangeException.idempotencyConflict();
            }
            if (existing.response() == null) {
                throw GoogleExchangeException.exchangeInProgress();
            }
            return existing.response();
        }

        if (replayEntries.containsKey(tokenHash)) {
            throw GoogleExchangeException.replayDetected();
        }

        enforceRateLimit(now);
        ensureCapacity(idempotencyEntries);
        ensureCapacity(replayEntries);
        Instant expiresAt = now.plus(ttl);
        idempotencyEntries.put(idempotencyHash, new IdempotencyEntry(tokenHash, null, expiresAt));
        replayEntries.put(tokenHash, new ReplayEntry(idempotencyHash, expiresAt));
        return null;
    }

    private synchronized void complete(
            String idempotencyHash,
            String tokenHash,
            GoogleExchangeResponse response) {
        IdempotencyEntry entry = idempotencyEntries.get(idempotencyHash);
        if (entry != null && entry.tokenHash().equals(tokenHash)) {
            idempotencyEntries.put(
                    idempotencyHash,
                    new IdempotencyEntry(tokenHash, response, entry.expiresAt()));
        }
    }

    private synchronized void abort(String idempotencyHash, String tokenHash) {
        idempotencyEntries.remove(idempotencyHash);
        ReplayEntry replay = replayEntries.get(tokenHash);
        if (replay != null && replay.idempotencyHash().equals(idempotencyHash)) {
            replayEntries.remove(tokenHash);
        }
    }

    private void enforceRateLimit(Instant now) {
        Instant cutoff = now.minus(rateLimitWindow);
        while (!requestTimes.isEmpty() && !requestTimes.peekFirst().isAfter(cutoff)) {
            requestTimes.removeFirst();
        }
        if (requestTimes.size() >= rateLimitRequests) {
            throw GoogleExchangeException.rateLimited();
        }
        requestTimes.addLast(now);
    }

    private void cleanup(Instant now) {
        idempotencyEntries.entrySet().removeIf(entry -> !entry.getValue().expiresAt().isAfter(now));
        replayEntries.entrySet().removeIf(entry -> !entry.getValue().expiresAt().isAfter(now));
    }

    private <T> void ensureCapacity(Map<String, T> entries) {
        if (entries.size() < maximumEntries) {
            return;
        }
        Iterator<String> iterator = entries.keySet().iterator();
        if (iterator.hasNext()) {
            iterator.next();
            iterator.remove();
        }
    }

    private String fingerprint(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8));
            return java.util.HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is unavailable", e);
        }
    }

    private record IdempotencyEntry(
            String tokenHash,
            GoogleExchangeResponse response,
            Instant expiresAt) {
    }

    private record ReplayEntry(String idempotencyHash, Instant expiresAt) {
    }
}
