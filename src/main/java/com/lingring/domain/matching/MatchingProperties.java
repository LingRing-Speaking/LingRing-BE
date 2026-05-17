package com.lingring.domain.matching;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "matching")
public record MatchingProperties(
        int confirmDeadlineSeconds,
        int cooldownMinutes
) {
}
