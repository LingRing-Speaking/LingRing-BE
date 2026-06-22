package com.lingring.global.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "matching")
public record MatchingProperties(
        int confirmDeadlineSeconds,
        int cooldownMinutes,
        int aliveTtlSeconds
) {

    public Duration aliveTtl() {
        return Duration.ofSeconds(aliveTtlSeconds);
    }
}
