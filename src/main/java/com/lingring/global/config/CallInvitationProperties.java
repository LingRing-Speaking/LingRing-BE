package com.lingring.global.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "call-invitation")
public record CallInvitationProperties(
        int inviteTtlSeconds,
        int resultTtlSeconds
) {

    public Duration inviteTtl() {
        return Duration.ofSeconds(inviteTtlSeconds);
    }

    public Duration resultTtl() {
        return Duration.ofSeconds(resultTtlSeconds);
    }
}
