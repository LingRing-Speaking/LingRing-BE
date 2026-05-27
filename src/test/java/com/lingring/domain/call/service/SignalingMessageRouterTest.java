package com.lingring.domain.call.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.then;

import com.lingring.domain.call.domain.Call;
import com.lingring.domain.call.domain.SignalingMessage;
import com.lingring.domain.call.domain.SignalingMessageType;
import com.lingring.global.config.ServiceIntegrationHelper;
import com.lingring.infrastructure.redis.RedisSignalingPublisher;
import java.time.LocalDateTime;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

class SignalingMessageRouterTest extends ServiceIntegrationHelper {

    private static final LocalDateTime STARTED_AT = LocalDateTime.of(2026, 4, 28, 10, 0);

    @Autowired
    private SignalingMessageRouter messageRouter;

    @MockitoSpyBean
    private RedisSignalingPublisher signalingPublisher;

    @Nested
    @DisplayName("forwardToCounterpart: 상대에게 메시지 포워드")
    class ForwardToCounterpart {

        @Test
        @DisplayName("sender→counterpart 라우팅을 채워 (roomId, message)로 publish한다")
        void forward_setsRoutingAndPublishes() {
            // given
            final UUID roomId = UUID.randomUUID();
            final Call call = Call.start(1L, 2L, roomId, STARTED_AT);
            final SignalingMessage offer = new SignalingMessage(
                    SignalingMessageType.OFFER, null, null, null);

            // when
            messageRouter.forwardToCounterpart(call, 1L, offer);

            // then
            final ArgumentCaptor<UUID> roomIdCaptor = ArgumentCaptor.forClass(UUID.class);
            final ArgumentCaptor<SignalingMessage> msgCaptor = ArgumentCaptor.forClass(SignalingMessage.class);
            then(signalingPublisher).should().publish(roomIdCaptor.capture(), msgCaptor.capture());
            assertThat(roomIdCaptor.getValue()).isEqualTo(roomId);
            assertThat(msgCaptor.getValue().fromUserId()).isEqualTo(1L);
            assertThat(msgCaptor.getValue().toUserId()).isEqualTo(2L);
            assertThat(msgCaptor.getValue().type()).isEqualTo(SignalingMessageType.OFFER);
        }
    }

    @Nested
    @DisplayName("publishHangup: HANGUP 메시지 합성/발행")
    class PublishHangup {

        @Test
        @DisplayName("from=disconnectedUser, to=counterpart인 HANGUP을 publish한다")
        void publishHangup_synthesizesHangupMessage() {
            // given
            final UUID roomId = UUID.randomUUID();
            final Call call = Call.start(1L, 2L, roomId, STARTED_AT);

            // when
            messageRouter.publishHangup(call, 1L);

            // then
            final ArgumentCaptor<UUID> roomIdCaptor = ArgumentCaptor.forClass(UUID.class);
            final ArgumentCaptor<SignalingMessage> msgCaptor = ArgumentCaptor.forClass(SignalingMessage.class);
            then(signalingPublisher).should().publish(roomIdCaptor.capture(), msgCaptor.capture());
            assertThat(roomIdCaptor.getValue()).isEqualTo(roomId);
            assertThat(msgCaptor.getValue().type()).isEqualTo(SignalingMessageType.HANGUP);
            assertThat(msgCaptor.getValue().fromUserId()).isEqualTo(1L);
            assertThat(msgCaptor.getValue().toUserId()).isEqualTo(2L);
        }
    }
}
