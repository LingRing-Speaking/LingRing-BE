package com.lingring.domain.matching;

import lombok.Getter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@ConfigurationProperties(prefix = "matching")
public class MatchingProperties {

    private final int confirmDeadlineSeconds;
    private final int cooldownMinutes;

    public MatchingProperties(final int confirmDeadlineSeconds, final int cooldownMinutes) {
        this.confirmDeadlineSeconds = confirmDeadlineSeconds;
        this.cooldownMinutes = cooldownMinutes;
    }
}
