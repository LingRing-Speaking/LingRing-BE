package com.lingring.infrastructure.s3;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "s3.call-recording")
public record CallRecordingS3Properties(
        String prefix,
        Duration uploadTtl
) {
}
