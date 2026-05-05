package com.lingring.infrastructure.s3;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

@TestConfiguration
public class FakeProfileImageStorageConfig {

    @Bean
    @Primary
    public FakeProfileImageStorage fakeProfileImageStorage() {
        return new FakeProfileImageStorage();
    }
}
