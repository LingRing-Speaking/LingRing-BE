package com.lingring.infrastructure.firebase;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

@TestConfiguration
public class FakePushSenderConfig {

    @Bean
    @Primary
    public FakePushSender fakePushSender() {
        return new FakePushSender();
    }
}
