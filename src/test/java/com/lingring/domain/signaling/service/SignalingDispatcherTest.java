package com.lingring.domain.signaling.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;

import com.lingring.domain.matching.dao.MatchRepository;
import com.lingring.domain.matching.domain.Match;
import com.lingring.domain.matching.domain.MatchStatus;
import com.lingring.domain.signaling.domain.SignalingMessage;
import com.lingring.domain.signaling.domain.SignalingMessageType;
import com.lingring.domain.signaling.dao.LocalSessionRegistry;
import com.lingring.global.config.ServiceIntegrationHelper;
import com.lingring.infrastructure.redis.SignalingChannels;
import java.time.LocalDateTime;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

class SignalingDispatcherTest extends ServiceIntegrationHelper {

    private static final LocalDateTime STARTED_AT = LocalDateTime.of(2026, 4, 27, 10, 0);

    @Autowired
    private SignalingDispatcher signalingDispatcher;

    @Autowired
    private MatchRepository matchRepository;

    @Autowired
    private LocalSessionRegistry sessionRegistry;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Nested
    @DisplayName("dispatch: 메시지 라우팅")
    class Dispatch {

        @Test
        @DisplayName("Match가 없으면 sender에게 ERROR 메시지를 전송한다")
        void dispatch_whenMatchNotFound_sendsErrorToSender() throws Exception {
            // given
            final Long senderId = 1L;
            final UUID roomId = UUID.randomUUID();
            final WebSocketSession senderSession = openSession();
            sessionRegistry.register(senderId, senderSession);

            final SignalingMessage message = new SignalingMessage(
                    SignalingMessageType.OFFER, roomId, null, null, null);

            // when
            signalingDispatcher.dispatch(senderId, message);

            // then
            final ArgumentCaptor<TextMessage> captor = ArgumentCaptor.forClass(TextMessage.class);
            then(senderSession).should().sendMessage(captor.capture());
            assertThat(captor.getValue().getPayload()).contains("ERROR");
            assertThat(captor.getValue().getPayload()).contains("MATCH_NOT_FOUND");
        }

        @Test
        @DisplayName("매칭 비참여자가 보내면 ERROR 메시지를 전송한다")
        void dispatch_whenSenderNotParticipant_sendsErrorToSender() throws Exception {
            // given
            final UUID roomId = UUID.randomUUID();
            matchRepository.save(Match.start(1L, 2L, roomId, STARTED_AT));

            final Long intruderId = 99L;
            final WebSocketSession intruderSession = openSession();
            sessionRegistry.register(intruderId, intruderSession);

            final SignalingMessage message = new SignalingMessage(
                    SignalingMessageType.OFFER, roomId, null, null, null);

            // when
            signalingDispatcher.dispatch(intruderId, message);

            // then
            final ArgumentCaptor<TextMessage> captor = ArgumentCaptor.forClass(TextMessage.class);
            then(intruderSession).should().sendMessage(captor.capture());
            assertThat(captor.getValue().getPayload()).contains("MATCH_PARTICIPANT_MISMATCH");
        }

        @Test
        @DisplayName("JOIN 한 명만 들어오면 joined SET에 한 명만 있고 READY는 발행되지 않는다")
        void dispatch_whenOnlyOneJoin_addsToJoinedSetOnly() {
            // given
            final UUID roomId = UUID.randomUUID();
            matchRepository.save(Match.start(1L, 2L, roomId, STARTED_AT));

            final SignalingMessage join = new SignalingMessage(
                    SignalingMessageType.JOIN, roomId, null, null, null);

            // when
            signalingDispatcher.dispatch(1L, join);

            // then
            assertThat(redisTemplate.opsForSet().size(SignalingChannels.joinedSetKey(roomId)))
                    .isEqualTo(1L);
        }

        @Test
        @DisplayName("JOIN 양쪽이 모두 들어오면 joined SET 크기가 2가 된다")
        void dispatch_whenBothJoin_joinedSetReachesTwo() {
            // given
            final UUID roomId = UUID.randomUUID();
            matchRepository.save(Match.start(1L, 2L, roomId, STARTED_AT));

            final SignalingMessage joinA = new SignalingMessage(
                    SignalingMessageType.JOIN, roomId, null, null, null);
            final SignalingMessage joinB = new SignalingMessage(
                    SignalingMessageType.JOIN, roomId, null, null, null);

            // when
            signalingDispatcher.dispatch(1L, joinA);
            signalingDispatcher.dispatch(2L, joinB);

            // then
            assertThat(redisTemplate.opsForSet().size(SignalingChannels.joinedSetKey(roomId)))
                    .isEqualTo(2L);
        }

        @Test
        @DisplayName("HANGUP을 받으면 Match가 ENDED 상태로 전이된다")
        @Transactional
        void dispatch_whenHangup_endsMatch() {
            // given
            final UUID roomId = UUID.randomUUID();
            matchRepository.save(Match.start(1L, 2L, roomId, STARTED_AT));

            final SignalingMessage hangup = new SignalingMessage(
                    SignalingMessageType.HANGUP, roomId, null, null, null);

            // when
            signalingDispatcher.dispatch(1L, hangup);

            // then
            final Match updated = matchRepository.findByRoomId(roomId).orElseThrow();
            assertThat(updated.getStatus()).isEqualTo(MatchStatus.ENDED);
            assertThat(updated.getEndedAt()).isNotNull();
        }

        @Test
        @DisplayName("HANGUP 처리 후 joined SET이 삭제된다")
        void dispatch_whenHangup_clearsJoinedSet() {
            // given
            final UUID roomId = UUID.randomUUID();
            matchRepository.save(Match.start(1L, 2L, roomId, STARTED_AT));
            redisTemplate.opsForSet().add(SignalingChannels.joinedSetKey(roomId), "1", "2");

            final SignalingMessage hangup = new SignalingMessage(
                    SignalingMessageType.HANGUP, roomId, null, null, null);

            // when
            signalingDispatcher.dispatch(1L, hangup);

            // then
            assertThat(redisTemplate.hasKey(SignalingChannels.joinedSetKey(roomId))).isFalse();
        }
    }

    @Nested
    @DisplayName("handleDisconnect: WebSocket 연결 종료 처리")
    class HandleDisconnect {

        @Test
        @DisplayName("참여자가 disconnect하면 Match가 ENDED 상태로 전이된다")
        @Transactional
        void handleDisconnect_endsMatch() {
            // given
            final UUID roomId = UUID.randomUUID();
            matchRepository.save(Match.start(1L, 2L, roomId, STARTED_AT));

            // when
            signalingDispatcher.handleDisconnect(1L, roomId);

            // then
            final Match updated = matchRepository.findByRoomId(roomId).orElseThrow();
            assertThat(updated.getStatus()).isEqualTo(MatchStatus.ENDED);
        }

        @Test
        @DisplayName("roomId가 null이면 아무 동작도 하지 않는다")
        void handleDisconnect_whenRoomIdNull_doesNothing() {
            // when & then
            signalingDispatcher.handleDisconnect(1L, null);
        }

        @Test
        @DisplayName("Match가 없으면 아무 동작도 하지 않는다 (이미 종료된 매치)")
        void handleDisconnect_whenMatchAbsent_doesNothing() {
            // when & then
            signalingDispatcher.handleDisconnect(1L, UUID.randomUUID());
        }
    }

    @Nested
    @DisplayName("deliverLocally: 인스턴스 로컬 전달")
    class DeliverLocally {

        @Test
        @DisplayName("toUserId 세션이 등록돼있으면 sendMessage가 호출된다")
        void deliverLocally_whenSessionExists_sendsMessage() throws Exception {
            // given
            final Long targetUserId = 2L;
            final WebSocketSession session = openSession();
            sessionRegistry.register(targetUserId, session);

            final SignalingMessage message = new SignalingMessage(
                    SignalingMessageType.OFFER, UUID.randomUUID(), 1L, targetUserId, null);

            // when
            signalingDispatcher.deliverLocally(message);

            // then
            then(session).should().sendMessage(any(TextMessage.class));
        }

        @Test
        @DisplayName("toUserId 세션이 없으면 sendMessage가 호출되지 않는다")
        void deliverLocally_whenSessionAbsent_doesNothing() throws Exception {
            // given: 세션 미등록
            final WebSocketSession unrelatedSession = openSession();
            sessionRegistry.register(99L, unrelatedSession);

            final SignalingMessage message = new SignalingMessage(
                    SignalingMessageType.OFFER, UUID.randomUUID(), 1L, 2L, null);

            // when
            signalingDispatcher.deliverLocally(message);

            // then
            then(unrelatedSession).should(never()).sendMessage(any(TextMessage.class));
        }

        @Test
        @DisplayName("toUserId가 null이면 sendMessage가 호출되지 않는다")
        void deliverLocally_whenToUserIdNull_doesNothing() throws Exception {
            // given
            final WebSocketSession session = openSession();
            sessionRegistry.register(1L, session);

            final SignalingMessage message = new SignalingMessage(
                    SignalingMessageType.OFFER, UUID.randomUUID(), 2L, null, null);

            // when
            signalingDispatcher.deliverLocally(message);

            // then
            then(session).should(never()).sendMessage(any(TextMessage.class));
        }
    }

    private WebSocketSession openSession() {
        final WebSocketSession session = mock(WebSocketSession.class);
        given(session.isOpen()).willReturn(true);
        return session;
    }
}
