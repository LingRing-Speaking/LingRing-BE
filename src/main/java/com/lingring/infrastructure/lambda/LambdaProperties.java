package com.lingring.infrastructure.lambda;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "aws.lambda")
public record LambdaProperties(
        String region,
        String transcriptFunctionName
) {
}
