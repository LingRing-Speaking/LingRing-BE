package com.lingring.global.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "presence")
public record PresenceProperties(
        int ttlSeconds
) {

    public Duration ttl() {
        return Duration.ofSeconds(ttlSeconds);
    }
}
