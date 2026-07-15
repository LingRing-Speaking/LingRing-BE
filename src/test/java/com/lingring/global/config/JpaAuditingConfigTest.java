package com.lingring.global.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.lingring.global.util.FixedDateTimeProvider;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAccessor;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class JpaAuditingConfigTest {

    @Test
    @DisplayName("auditing 시각은 JVM 기본 타임존이 아니라 주입된 DateTimeProvider의 시각을 반환한다")
    void auditingDateTimeProvider_returnsInjectedProviderTime() {
        // given
        final LocalDateTime fixed = LocalDateTime.of(2026, 7, 15, 17, 30, 0);
        final JpaAuditingConfig config = new JpaAuditingConfig();

        // when
        final Optional<TemporalAccessor> now = config
                .auditingDateTimeProvider(new FixedDateTimeProvider(fixed))
                .getNow();

        // then
        assertThat(now).contains(fixed);
    }
}
