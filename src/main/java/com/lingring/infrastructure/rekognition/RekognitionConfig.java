package com.lingring.infrastructure.rekognition;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.rekognition.RekognitionClient;

@Configuration
@EnableConfigurationProperties(RekognitionProperties.class)
public class RekognitionConfig {

    @Bean
    public RekognitionClient rekognitionClient(final RekognitionProperties properties) {
        return RekognitionClient.builder()
                .region(Region.of(properties.region()))
                .build();
    }
}
