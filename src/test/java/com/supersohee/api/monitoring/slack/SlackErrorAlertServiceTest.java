package com.supersohee.api.monitoring.slack;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.servlet.HandlerMapping;

import java.net.URI;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class SlackErrorAlertServiceTest {

    @Test
    void disabledConfigurationIsANoOp() {
        SlackAlertProperties properties = enabledProperties();
        properties.setEnabled(false);
        SlackWebhookClient client = mock(SlackWebhookClient.class);
        SlackErrorAlertService service = service(properties, client, Runnable::run, mutableClock());

        service.report("GET", "/api/test", 500, failure());

        verify(client, never()).send(any(), any());
    }

    @Test
    void blankWebhookIsANoOpEvenWhenEnabled() {
        SlackAlertProperties properties = enabledProperties();
        properties.setWebhookUrl("");
        SlackWebhookClient client = mock(SlackWebhookClient.class);
        SlackErrorAlertService service = service(properties, client, Runnable::run, mutableClock());

        service.report("GET", "/api/test", 500, failure());

        verify(client, never()).send(any(), any());
    }

    @Test
    void payloadUsesOnlySafeRequestAndFailureMetadata() {
        SlackWebhookClient client = mock(SlackWebhookClient.class);
        SlackErrorAlertService service = service(enabledProperties(), client, Runnable::run, mutableClock());
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/users/private-user-id");
        request.setQueryString("token=query-secret");
        request.addHeader("Authorization", "Bearer jwt-secret");
        request.addHeader("Cookie", "session=cookie-secret");
        request.setAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE, "/api/users/{userId}");

        service.report(request, 500, new IllegalStateException("exception-message-secret"));

        ArgumentCaptor<String> text = ArgumentCaptor.forClass(String.class);
        verify(client).send(any(URI.class), text.capture());
        assertThat(text.getValue())
                .contains("service=supersohee-api")
                .contains("environment=test")
                .contains("status=500")
                .contains("request=GET /api/users/{userId}")
                .contains("exception=java.lang.IllegalStateException")
                .doesNotContain("private-user-id")
                .doesNotContain("query-secret")
                .doesNotContain("jwt-secret")
                .doesNotContain("cookie-secret")
                .doesNotContain("exception-message-secret");
    }

    @Test
    void duplicateIsSuppressedUntilWindowExpiresAndThenReportsCount() {
        SlackWebhookClient client = mock(SlackWebhookClient.class);
        MutableClock clock = mutableClock();
        SlackErrorAlertService service = service(enabledProperties(), client, Runnable::run, clock);
        RuntimeException sameFailure = failure();

        service.report("GET", "/api/test", 500, sameFailure);
        service.report("GET", "/api/test", 500, sameFailure);
        verify(client, times(1)).send(any(), any());

        clock.advance(Duration.ofMinutes(5).plusSeconds(1));
        service.report("GET", "/api/test", 500, sameFailure);

        ArgumentCaptor<String> text = ArgumentCaptor.forClass(String.class);
        verify(client, times(2)).send(any(), text.capture());
        assertThat(text.getAllValues().get(1)).contains("suppressedDuplicates=1");
    }

    @Test
    void globalRateLimitAllowsOnlyTenAlertsPerMinute() {
        SlackAlertProperties properties = enabledProperties();
        SlackWebhookClient client = mock(SlackWebhookClient.class);
        SlackErrorAlertService service = service(properties, client, Runnable::run, mutableClock());

        for (int index = 0; index < 11; index++) {
            service.report("GET", "/api/test/" + index, 500, failure());
        }

        verify(client, times(10)).send(any(), any());
    }

    @Test
    void dedupeCacheNeverRetainsMoreThanConfiguredEntries() {
        SlackAlertProperties properties = enabledProperties();
        properties.setCacheSize(500);
        properties.setMaxPerMinute(1_000);
        SlackWebhookClient client = mock(SlackWebhookClient.class);
        SlackErrorAlertService service = service(properties, client, Runnable::run, mutableClock());
        RuntimeException sameFailure = failure();

        for (int index = 0; index < 501; index++) {
            service.report("GET", "/api/test/" + index, 500, sameFailure);
        }
        service.report("GET", "/api/test/0", 500, sameFailure);

        verify(client, times(502)).send(any(), any());
    }

    @Test
    void rejectedQueueNeverAffectsCaller() {
        Executor rejectingExecutor = task -> {
            throw new RejectedExecutionException("full");
        };
        SlackErrorAlertService service = service(
                enabledProperties(), mock(SlackWebhookClient.class), rejectingExecutor, mutableClock());

        assertThatCode(() -> service.report("POST", "/api/test", 500, failure()))
                .doesNotThrowAnyException();
    }

    @Test
    void fourHundredResponsesAreIgnored() {
        SlackWebhookClient client = mock(SlackWebhookClient.class);
        SlackErrorAlertService service = service(enabledProperties(), client, Runnable::run, mutableClock());

        service.report("POST", "/api/test", 400, failure());

        verify(client, never()).send(any(), any());
    }

    private SlackErrorAlertService service(
            SlackAlertProperties properties,
            SlackWebhookClient client,
            Executor executor,
            Clock clock) {
        return new SlackErrorAlertService(properties, client, executor, clock);
    }

    private SlackAlertProperties enabledProperties() {
        SlackAlertProperties properties = new SlackAlertProperties();
        properties.setEnabled(true);
        properties.setWebhookUrl("https://hooks.slack.com/services/test/webhook/value");
        properties.setEnvironment("test");
        return properties;
    }

    private RuntimeException failure() {
        return new RuntimeException("must-not-be-sent");
    }

    private MutableClock mutableClock() {
        return new MutableClock(Instant.parse("2026-09-02T00:00:00Z"));
    }

    private static final class MutableClock extends Clock {
        private Instant instant;

        private MutableClock(Instant instant) {
            this.instant = instant;
        }

        private void advance(Duration duration) {
            instant = instant.plus(duration);
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return instant;
        }
    }
}
