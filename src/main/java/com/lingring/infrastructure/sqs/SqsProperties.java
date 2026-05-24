package com.lingring.infrastructure.sqs;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "aws.sqs")
public record SqsProperties(
        String region,
        String callAnalysisResultQueueUrl,
        long pollIntervalMs
) {
}
