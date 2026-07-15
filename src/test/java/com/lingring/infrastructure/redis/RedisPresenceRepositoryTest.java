package com.lingring.infrastructure.redis;

import static org.assertj.core.api.Assertions.assertThat;

import com.lingring.domain.presence.dao.PresenceRepository;
import com.lingring.global.config.ServiceIntegrationHelper;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class RedisPresenceRepositoryTest extends ServiceIntegrationHelper {

    private static final Duration TTL = Duration.ofSeconds(10);

    @Autowired
    private PresenceRepository presenceRepository;

    @Nested
    @DisplayName("findOnlineUserIds는")
    class FindOnlineUserIds {

        @Test
        @DisplayName("markOnline된 사용자만 온라인으로 반환한다")
        void findOnlineUserIds_whenMixed_returnsOnlyOnlineUsers() {
            // given
            presenceRepository.markOnline(1L, TTL);
            presenceRepository.markOnline(3L, TTL);

            // when
            final var onlineUserIds = presenceRepository.findOnlineUserIds(List.of(1L, 2L, 3L));

            // then
            assertThat(onlineUserIds).containsExactlyInAnyOrder(1L, 3L);
        }

        @Test
        @DisplayName("빈 목록 조회 시 빈 집합을 반환한다")
        void findOnlineUserIds_whenEmptyInput_returnsEmptySet() {
            // when
            final var onlineUserIds = presenceRepository.findOnlineUserIds(List.of());

            // then
            assertThat(onlineUserIds).isEmpty();
        }
    }

    @Test
    @DisplayName("markOffline하면 즉시 오프라인으로 조회된다")
    void markOffline_removesPresenceImmediately() {
        // given
        presenceRepository.markOnline(1L, TTL);

        // when
        presenceRepository.markOffline(1L);

        // then
        assertThat(presenceRepository.findOnlineUserIds(List.of(1L))).isEmpty();
    }

    @Test
    @DisplayName("TTL이 지나면 오프라인으로 조회된다")
    void markOnline_afterTtlExpires_userIsOffline() throws InterruptedException {
        // given
        presenceRepository.markOnline(1L, Duration.ofMillis(200));

        // when
        Thread.sleep(400);

        // then
        assertThat(presenceRepository.findOnlineUserIds(List.of(1L))).isEmpty();
    }

    @Test
    @DisplayName("markOnline을 다시 호출하면 TTL이 연장된다")
    void markOnline_whenCalledAgain_extendsTtl() throws InterruptedException {
        // given
        presenceRepository.markOnline(1L, Duration.ofMillis(400));

        // when: 만료 전 재호출로 TTL 연장
        Thread.sleep(200);
        presenceRepository.markOnline(1L, Duration.ofMillis(400));
        Thread.sleep(300);

        // then: 최초 TTL(400ms)은 지났지만 연장되어 여전히 온라인
        assertThat(presenceRepository.findOnlineUserIds(List.of(1L))).containsExactly(1L);
    }
}
