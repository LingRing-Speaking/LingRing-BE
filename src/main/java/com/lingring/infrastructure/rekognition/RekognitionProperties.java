package com.lingring.infrastructure.rekognition;

import java.util.Set;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "aws.rekognition")
public record RekognitionProperties(
        String region,
        Float minConfidence,
        Set<String> blockedCategories
) {
}
