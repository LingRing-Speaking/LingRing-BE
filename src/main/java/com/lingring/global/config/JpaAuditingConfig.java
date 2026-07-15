package com.lingring.global.config;

import com.lingring.global.util.DateTimeProvider;
import java.util.Optional;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@Configuration
@EnableJpaAuditing(dateTimeProviderRef = "auditingDateTimeProvider")
public class JpaAuditingConfig {

    // 기본 CurrentDateTimeProvider는 JVM 기본 타임존을 타므로 Asia/Seoul 고정 provider로 대체
    @Bean
    public org.springframework.data.auditing.DateTimeProvider auditingDateTimeProvider(
            final DateTimeProvider dateTimeProvider
    ) {
        return () -> Optional.of(dateTimeProvider.now());
    }
}
