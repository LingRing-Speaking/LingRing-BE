package com.lingring.infrastructure.redis;

import static org.assertj.core.api.Assertions.assertThat;

import com.lingring.domain.matching.dao.PairCooldownRepository;
import com.lingring.domain.matching.scheduler.MatchConfirmationExpiryWorker;
import com.lingring.domain.matching.scheduler.MatchingWorker;
import com.lingring.global.config.ServiceIntegrationHelper;
import java.time.Duration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

class RedisPairCooldownRepositoryTest extends ServiceIntegrationHelper {

    @Autowired
    private PairCooldownRepository repository;

    @MockitoBean
    @SuppressWarnings("unused")
    private MatchingWorker matchingWorker;

    @MockitoBean
    @SuppressWarnings("unused")
    private MatchConfirmationExpiryWorker matchConfirmationExpiryWorker;

    @Test
    @DisplayName("put 후 contains는 true, 다른 페어는 false")
    void put_andContains() {
        // when
        repository.put("1:2", Duration.ofMinutes(10));

        // then
        assertThat(repository.contains("1:2")).isTrue();
        assertThat(repository.contains("1:3")).isFalse();
    }
}
