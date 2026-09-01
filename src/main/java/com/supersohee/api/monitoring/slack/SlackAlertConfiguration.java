package com.supersohee.api.monitoring.slack;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Clock;
import java.time.Duration;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@Configuration
@EnableConfigurationProperties(SlackAlertProperties.class)
public class SlackAlertConfiguration {

    @Bean
    SlackWebhookClient slackWebhookClient(SlackAlertProperties properties) {
        Duration timeout = positiveDuration(properties.getTimeout(), Duration.ofSeconds(2));
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(timeout)
                .build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(timeout);
        return new SlackWebhookClient(RestClient.builder().requestFactory(requestFactory).build());
    }

    @Bean(name = "slackAlertExecutor", destroyMethod = "shutdown")
    ExecutorService slackAlertExecutor(SlackAlertProperties properties) {
        int queueCapacity = Math.max(1, properties.getQueueCapacity());
        AtomicInteger threadNumber = new AtomicInteger();
        return new ThreadPoolExecutor(
                1,
                1,
                0L,
                TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<>(queueCapacity),
                runnable -> {
                    Thread thread = new Thread(runnable, "slack-error-alert-" + threadNumber.incrementAndGet());
                    thread.setDaemon(true);
                    return thread;
                },
                new ThreadPoolExecutor.AbortPolicy());
    }

    @Bean
    SlackErrorAlertService slackErrorAlertService(
            SlackAlertProperties properties,
            SlackWebhookClient webhookClient,
            @Qualifier("slackAlertExecutor") Executor executor) {
        return new SlackErrorAlertService(properties, webhookClient, executor, Clock.systemUTC());
    }

    private Duration positiveDuration(Duration configured, Duration fallback) {
        return configured == null || configured.isZero() || configured.isNegative() ? fallback : configured;
    }
}
