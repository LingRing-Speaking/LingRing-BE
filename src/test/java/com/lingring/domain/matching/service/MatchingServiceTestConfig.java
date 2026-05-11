package com.lingring.domain.matching.service;

import com.lingring.global.util.FixedDateTimeProvider;
import java.time.LocalDateTime;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

@TestConfiguration
class MatchingServiceTestConfig {

    @Primary
    @Bean
    FixedDateTimeProvider dateTimeProvider() {
        return new FixedDateTimeProvider(LocalDateTime.of(2026, 5, 12, 12, 0, 0));
    }
}
