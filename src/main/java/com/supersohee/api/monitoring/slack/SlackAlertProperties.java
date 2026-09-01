package com.supersohee.api.monitoring.slack;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "alerts.slack")
public class SlackAlertProperties {

    private boolean enabled;
    private String webhookUrl = "";
    private String environment = "unknown";
    private Duration timeout = Duration.ofSeconds(2);
    private Duration dedupeWindow = Duration.ofMinutes(5);
    private int maxPerMinute = 10;
    private int cacheSize = 500;
    private int queueCapacity = 100;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getWebhookUrl() {
        return webhookUrl;
    }

    public void setWebhookUrl(String webhookUrl) {
        this.webhookUrl = webhookUrl;
    }

    public String getEnvironment() {
        return environment;
    }

    public void setEnvironment(String environment) {
        this.environment = environment;
    }

    public Duration getTimeout() {
        return timeout;
    }

    public void setTimeout(Duration timeout) {
        this.timeout = timeout;
    }

    public Duration getDedupeWindow() {
        return dedupeWindow;
    }

    public void setDedupeWindow(Duration dedupeWindow) {
        this.dedupeWindow = dedupeWindow;
    }

    public int getMaxPerMinute() {
        return maxPerMinute;
    }

    public void setMaxPerMinute(int maxPerMinute) {
        this.maxPerMinute = maxPerMinute;
    }

    public int getCacheSize() {
        return cacheSize;
    }

    public void setCacheSize(int cacheSize) {
        this.cacheSize = cacheSize;
    }

    public int getQueueCapacity() {
        return queueCapacity;
    }

    public void setQueueCapacity(int queueCapacity) {
        this.queueCapacity = queueCapacity;
    }
}
