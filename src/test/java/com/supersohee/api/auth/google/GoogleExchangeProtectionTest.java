package com.supersohee.api.auth.google;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GoogleExchangeProtectionTest {

    private static final String EXCHANGE_KEY = "exchange-key-that-is-at-least-thirty-two-bytes";

    @Test
    void returnsCachedResponseForSameIdempotencyKeyAndToken() {
        GoogleExchangeProtection protection = protection(10);
        AtomicInteger calls = new AtomicInteger();

        GoogleExchangeResponse first = protection.execute(
                EXCHANGE_KEY,
                "request-1",
                "id-token-1",
                () -> response(calls.incrementAndGet()));
        GoogleExchangeResponse retry = protection.execute(
                EXCHANGE_KEY,
                "request-1",
                "id-token-1",
                () -> response(calls.incrementAndGet()));

        assertThat(retry).isEqualTo(first);
        assertThat(calls).hasValue(1);
    }

    @Test
    void rejectsSameTokenWithAnotherIdempotencyKey() {
        GoogleExchangeProtection protection = protection(10);
        protection.execute(EXCHANGE_KEY, "request-1", "id-token-1", () -> response(1));

        assertThatThrownBy(() -> protection.execute(
                EXCHANGE_KEY,
                "request-2",
                "id-token-1",
                () -> response(2)))
                .isInstanceOfSatisfying(GoogleExchangeException.class,
                        exception -> assertThat(exception.code()).isEqualTo("TOKEN_REPLAY_DETECTED"));
    }

    @Test
    void rejectsIdempotencyKeyReusedForAnotherToken() {
        GoogleExchangeProtection protection = protection(10);
        protection.execute(EXCHANGE_KEY, "request-1", "id-token-1", () -> response(1));

        assertThatThrownBy(() -> protection.execute(
                EXCHANGE_KEY,
                "request-1",
                "id-token-2",
                () -> response(2)))
                .isInstanceOfSatisfying(GoogleExchangeException.class,
                        exception -> assertThat(exception.code()).isEqualTo("IDEMPOTENCY_CONFLICT"));
    }

    @Test
    void enforcesProcessLocalRateLimit() {
        GoogleExchangeProtection protection = protection(1);
        protection.execute(EXCHANGE_KEY, "request-1", "id-token-1", () -> response(1));

        assertThatThrownBy(() -> protection.execute(
                EXCHANGE_KEY,
                "request-2",
                "id-token-2",
                () -> response(2)))
                .isInstanceOfSatisfying(GoogleExchangeException.class,
                        exception -> assertThat(exception.code()).isEqualTo("RATE_LIMITED"));
    }

    private GoogleExchangeProtection protection(int requestsPerWindow) {
        return new GoogleExchangeProtection(
                EXCHANGE_KEY,
                Duration.ofMinutes(5),
                100,
                requestsPerWindow,
                Duration.ofMinutes(1),
                Clock.fixed(Instant.parse("2026-08-29T00:00:00Z"), ZoneOffset.UTC));
    }

    private GoogleExchangeResponse response(int sequence) {
        return new GoogleExchangeResponse("token-" + sequence, "user-" + sequence);
    }
}
