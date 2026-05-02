package com.lingring.domain.signaling.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.lingring.domain.call.domain.Call;
import com.lingring.global.config.ServiceIntegrationHelper;
import com.lingring.infrastructure.redis.SignalingChannels;
import java.time.LocalDateTime;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;

class SignalingReadyCoordinatorTest extends ServiceIntegrationHelper {

    private static final LocalDateTime STARTED_AT = LocalDateTime.of(2026, 4, 28, 10, 0);

    @Autowired
    private SignalingReadyCoordinator readyCoordinator;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Nested
    @DisplayName("recordJoinAndAnnounceIfReady: JOIN→READY 상태 머신")
    class RecordJoin {

        @Test
        @DisplayName("한 명만 JOIN하면 joinedSet 크기가 1이고 READY는 발행되지 않는다")
        void onlyOneJoin_addsToJoinedSetOnly() {
            // given
            final UUID roomId = UUID.randomUUID();
            final Call call = Call.start(1L, 2L, roomId, STARTED_AT);

            // when
            readyCoordinator.recordJoinAndAnnounceIfReady(call, 1L);

            // then
            assertThat(redisTemplate.opsForSet().size(SignalingChannels.joinedSetKey(roomId)))
                    .isEqualTo(1L);
            assertThat(redisTemplate.hasKey(SignalingChannels.readyLockKey(roomId))).isFalse();
        }

        @Test
        @DisplayName("두 명 모두 JOIN하면 joinedSet 크기가 2가 되고 readyLock이 잡힌다")
        void bothJoin_acquiresReadyLock() {
            // given
            final UUID roomId = UUID.randomUUID();
            final Call call = Call.start(1L, 2L, roomId, STARTED_AT);

            // when
            readyCoordinator.recordJoinAndAnnounceIfReady(call, 1L);
            readyCoordinator.recordJoinAndAnnounceIfReady(call, 2L);

            // then
            assertThat(redisTemplate.opsForSet().size(SignalingChannels.joinedSetKey(roomId)))
                    .isEqualTo(2L);
            assertThat(redisTemplate.hasKey(SignalingChannels.readyLockKey(roomId))).isTrue();
        }
    }

    @Nested
    @DisplayName("cleanupRoom: 룸 정리")
    class CleanupRoom {

        @Test
        @DisplayName("joinedSet과 readyLock을 모두 삭제한다")
        void cleanup_deletesBothKeys() {
            // given
            final UUID roomId = UUID.randomUUID();
            redisTemplate.opsForSet().add(SignalingChannels.joinedSetKey(roomId), "1", "2");
            redisTemplate.opsForValue().set(SignalingChannels.readyLockKey(roomId), "1");

            // when
            readyCoordinator.cleanupRoom(roomId);

            // then
            assertThat(redisTemplate.hasKey(SignalingChannels.joinedSetKey(roomId))).isFalse();
            assertThat(redisTemplate.hasKey(SignalingChannels.readyLockKey(roomId))).isFalse();
        }
    }
}
