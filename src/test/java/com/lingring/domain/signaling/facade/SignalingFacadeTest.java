package com.lingring.domain.signaling.facade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.lingring.domain.call.dao.CallHistoryRepository;
import com.lingring.domain.call.domain.CallHistory;
import com.lingring.domain.call.exception.CallHistoryNotFoundException;
import com.lingring.domain.signaling.domain.SignalingMessage;
import com.lingring.domain.signaling.domain.SignalingMessageType;
import com.lingring.global.config.ServiceIntegrationHelper;
import com.lingring.infrastructure.redis.SignalingChannels;
import java.time.LocalDateTime;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.transaction.annotation.Transactional;

class SignalingFacadeTest extends ServiceIntegrationHelper {

    private static final LocalDateTime STARTED_AT = LocalDateTime.of(2026, 4, 28, 10, 0);

    @Autowired
    private SignalingFacade signalingFacade;

    @Autowired
    private CallHistoryRepository callHistoryRepository;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Nested
    @DisplayName("dispatch: 메시지 라우팅")
    class Dispatch {

        @Test
        @DisplayName("CallHistory가 없으면 CallHistoryNotFoundException을 던진다 (핸드셰이크 invariant 위반)")
        void dispatch_whenCallHistoryNotFound_throws() {
            // given
            final SignalingMessage message = new SignalingMessage(
                    SignalingMessageType.OFFER, null, null, null);

            // when & then
            assertThatThrownBy(() -> signalingFacade.dispatch(1L, UUID.randomUUID(), message))
                    .isInstanceOf(CallHistoryNotFoundException.class);
        }

        @Test
        @DisplayName("HANGUP을 받으면 CallHistory가 종료 상태로 전이된다")
        @Transactional
        void dispatch_whenHangup_endsCall() {
            // given
            final UUID roomId = UUID.randomUUID();
            callHistoryRepository.save(CallHistory.start(1L, 2L, roomId, STARTED_AT));
            final SignalingMessage hangup = new SignalingMessage(
                    SignalingMessageType.HANGUP, null, null, null);

            // when
            signalingFacade.dispatch(1L, roomId, hangup);

            // then
            final CallHistory updated = callHistoryRepository.findByRoomId(roomId).orElseThrow();
            assertThat(updated.isActive()).isFalse();
            assertThat(updated.getEndedAt()).isNotNull();
        }

        @Test
        @DisplayName("HANGUP 처리 후 joinedSet과 readyLock이 모두 삭제된다")
        void dispatch_whenHangup_cleansUpRoom() {
            // given
            final UUID roomId = UUID.randomUUID();
            callHistoryRepository.save(CallHistory.start(1L, 2L, roomId, STARTED_AT));
            redisTemplate.opsForSet().add(SignalingChannels.joinedSetKey(roomId), "1", "2");
            redisTemplate.opsForValue().set(SignalingChannels.readyLockKey(roomId), "1");
            final SignalingMessage hangup = new SignalingMessage(
                    SignalingMessageType.HANGUP, null, null, null);

            // when
            signalingFacade.dispatch(1L, roomId, hangup);

            // then
            assertThat(redisTemplate.hasKey(SignalingChannels.joinedSetKey(roomId))).isFalse();
            assertThat(redisTemplate.hasKey(SignalingChannels.readyLockKey(roomId))).isFalse();
        }
    }

    @Nested
    @DisplayName("handleDisconnect: 연결 종료 처리")
    class HandleDisconnect {

        @Test
        @DisplayName("참여자가 disconnect하면 CallHistory가 종료된다")
        @Transactional
        void handleDisconnect_endsCall() {
            // given
            final UUID roomId = UUID.randomUUID();
            callHistoryRepository.save(CallHistory.start(1L, 2L, roomId, STARTED_AT));

            // when
            signalingFacade.handleDisconnect(1L, roomId);

            // then
            final CallHistory updated = callHistoryRepository.findByRoomId(roomId).orElseThrow();
            assertThat(updated.isActive()).isFalse();
        }

        @Test
        @DisplayName("roomId가 null이면 아무 동작도 하지 않는다")
        void handleDisconnect_whenRoomIdNull_doesNothing() {
            // when & then
            signalingFacade.handleDisconnect(1L, null);
        }

        @Test
        @DisplayName("CallHistory가 없으면 아무 동작도 하지 않는다 (이미 종료된 통화)")
        void handleDisconnect_whenCallHistoryAbsent_doesNothing() {
            // when & then
            signalingFacade.handleDisconnect(1L, UUID.randomUUID());
        }
    }
}
