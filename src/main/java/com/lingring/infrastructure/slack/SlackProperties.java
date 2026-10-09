package com.lingring.infrastructure.slack;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "slack")
public record SlackProperties(
        String matchingWebhookUrl
) {

    public boolean hasMatchingWebhookUrl() {
        return matchingWebhookUrl != null && !matchingWebhookUrl.isBlank();
    }
}
