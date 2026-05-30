package com.lingring.global.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "test-redis")
public record RedisContainerProperties(
        String image
) {
}
