package com.lingring.infrastructure.rekognition;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

@TestConfiguration
public class FakeProfileImageModeratorConfig {

    @Bean
    @Primary
    public FakeProfileImageModerator fakeProfileImageModerator() {
        return new FakeProfileImageModerator();
    }
}
