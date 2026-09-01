package com.supersohee.api.monitoring.slack;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.net.URI;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class SlackWebhookClientTest {

    private static final URI WEBHOOK_URI = URI.create("https://hooks.slack.com/services/test/webhook/value");

    @Test
    void sendsOnlyTextJsonWithoutRetry() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        SlackWebhookClient client = new SlackWebhookClient(builder.build());
        server.expect(once(), requestTo(WEBHOOK_URI))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().json("{\"text\":\"safe alert\"}"))
                .andRespond(withSuccess());

        client.send(WEBHOOK_URI, "safe alert");

        server.verify();
    }

    @Test
    void remoteFailureDoesNotEscapeOrRetry() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        SlackWebhookClient client = new SlackWebhookClient(builder.build());
        server.expect(once(), requestTo(WEBHOOK_URI)).andRespond(withServerError());

        assertThatCode(() -> client.send(WEBHOOK_URI, "safe alert"))
                .doesNotThrowAnyException();
        server.verify();
    }
}
