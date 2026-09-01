package com.supersohee.api.monitoring.slack;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.net.URI;
import java.util.Map;

class SlackWebhookClient {

    private static final Logger log = LoggerFactory.getLogger(SlackWebhookClient.class);

    private final RestClient restClient;

    SlackWebhookClient(RestClient restClient) {
        this.restClient = restClient;
    }

    void send(URI webhookUri, String text) {
        try {
            restClient.post()
                    .uri(webhookUri)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("text", text))
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientResponseException failure) {
            log.warn("Slack error alert delivery failed with HTTP status {}", failure.getStatusCode().value());
        } catch (RuntimeException failure) {
            log.warn("Slack error alert delivery failed ({})", failure.getClass().getSimpleName());
        }
    }
}
