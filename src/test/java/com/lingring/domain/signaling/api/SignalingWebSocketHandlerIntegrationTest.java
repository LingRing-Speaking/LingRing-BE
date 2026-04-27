package com.lingring.domain.signaling.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.lingring.domain.matching.dao.MatchRepository;
import com.lingring.domain.matching.domain.Match;
import com.lingring.domain.matching.domain.MatchStatus;
import com.lingring.domain.signaling.domain.SignalingMessage;
import com.lingring.domain.signaling.domain.SignalingMessageType;
import com.lingring.global.config.DataInitializer;
import com.lingring.global.config.TestContainersTest;
import java.net.URI;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@TestContainersTest
class SignalingWebSocketHandlerIntegrationTest {

    private static final LocalDateTime STARTED_AT = LocalDateTime.of(2026, 4, 27, 10, 0);
    private static final long AWAIT_SECONDS = 3L;

    @LocalServerPort
    private int port;

    @Autowired
    private DataInitializer dataInitializer;

    @Autowired
    private RedisConnectionFactory redisConnectionFactory;

    @Autowired
    private MatchRepository matchRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        dataInitializer.deleteAll();
        redisConnectionFactory.getConnection().serverCommands().flushDb();
    }

    @Test
    @DisplayName("두 클라이언트가 연결 후 JOIN→READY→OFFER→ANSWER→HANGUP 전 흐름을 통과한다")
    void fullSignalingHandshake() throws Exception {
        // given
        final UUID roomId = UUID.randomUUID();
        matchRepository.save(Match.start(1L, 2L, roomId, STARTED_AT));

        final CollectingHandler handlerA = new CollectingHandler(objectMapper);
        final CollectingHandler handlerB = new CollectingHandler(objectMapper);

        final WebSocketSession sessionA = connect(1L, roomId, handlerA);
        final WebSocketSession sessionB = connect(2L, roomId, handlerB);

        // when: 양쪽 JOIN
        sendMessage(sessionA, new SignalingMessage(SignalingMessageType.JOIN, roomId, null, null, null));
        sendMessage(sessionB, new SignalingMessage(SignalingMessageType.JOIN, roomId, null, null, null));

        // then: 양쪽이 READY 수신, caller=1L, callee=2L
        final SignalingMessage readyForA = handlerA.awaitNext();
        final SignalingMessage readyForB = handlerB.awaitNext();
        assertThat(readyForA.type()).isEqualTo(SignalingMessageType.READY);
        assertThat(readyForB.type()).isEqualTo(SignalingMessageType.READY);
        assertThat(readyForA.payload().get("callerUserId").asLong()).isEqualTo(1L);
        assertThat(readyForA.payload().get("calleeUserId").asLong()).isEqualTo(2L);

        // OFFER A → B
        sendMessage(sessionA, offerOrAnswer(SignalingMessageType.OFFER, roomId, "offer-sdp"));
        final SignalingMessage offerForB = handlerB.awaitNext();
        assertThat(offerForB.type()).isEqualTo(SignalingMessageType.OFFER);
        assertThat(offerForB.fromUserId()).isEqualTo(1L);
        assertThat(offerForB.toUserId()).isEqualTo(2L);
        assertThat(offerForB.payload().get("sdp").asString()).isEqualTo("offer-sdp");

        // ANSWER B → A
        sendMessage(sessionB, offerOrAnswer(SignalingMessageType.ANSWER, roomId, "answer-sdp"));
        final SignalingMessage answerForA = handlerA.awaitNext();
        assertThat(answerForA.type()).isEqualTo(SignalingMessageType.ANSWER);
        assertThat(answerForA.fromUserId()).isEqualTo(2L);
        assertThat(answerForA.toUserId()).isEqualTo(1L);

        // HANGUP A → B
        sendMessage(sessionA, new SignalingMessage(SignalingMessageType.HANGUP, roomId, null, null, null));
        final SignalingMessage hangupForB = handlerB.awaitNext();
        assertThat(hangupForB.type()).isEqualTo(SignalingMessageType.HANGUP);

        // Match가 ENDED로 전이됨
        Awaitility.await().atMost(Duration.ofSeconds(AWAIT_SECONDS))
                .untilAsserted(() -> assertThat(matchRepository.findByRoomId(roomId).orElseThrow().getStatus())
                        .isEqualTo(MatchStatus.ENDED));

        sessionA.close();
        sessionB.close();
    }

    @Test
    @DisplayName("일방적 disconnect 시 상대에게 HANGUP이 전달되고 Match가 ENDED로 전이된다")
    void disconnect_propagatesHangupAndEndsMatch() throws Exception {
        // given
        final UUID roomId = UUID.randomUUID();
        matchRepository.save(Match.start(1L, 2L, roomId, STARTED_AT));

        final CollectingHandler handlerA = new CollectingHandler(objectMapper);
        final CollectingHandler handlerB = new CollectingHandler(objectMapper);

        final WebSocketSession sessionA = connect(1L, roomId, handlerA);
        final WebSocketSession sessionB = connect(2L, roomId, handlerB);

        // when: A가 일방적으로 close
        sessionA.close();

        // then: B가 HANGUP 수신
        final SignalingMessage hangupForB = handlerB.awaitNext();
        assertThat(hangupForB.type()).isEqualTo(SignalingMessageType.HANGUP);

        Awaitility.await().atMost(Duration.ofSeconds(AWAIT_SECONDS))
                .untilAsserted(() -> assertThat(matchRepository.findByRoomId(roomId).orElseThrow().getStatus())
                        .isEqualTo(MatchStatus.ENDED));

        sessionB.close();
    }

    @Test
    @DisplayName("존재하지 않는 roomId로 연결을 시도하면 핸드셰이크가 실패한다")
    void connect_whenRoomNotFound_handshakeFails() {
        // when & then
        final UUID unknownRoomId = UUID.randomUUID();
        assertThatThrownBy(() -> connect(1L, unknownRoomId, new CollectingHandler(objectMapper)))
                .hasCauseInstanceOf(jakarta.websocket.DeploymentException.class);
    }

    @Test
    @DisplayName("매칭 비참여자가 연결을 시도하면 핸드셰이크가 실패한다")
    void connect_whenNotParticipant_handshakeFails() {
        // given
        final UUID roomId = UUID.randomUUID();
        matchRepository.save(Match.start(1L, 2L, roomId, STARTED_AT));

        // when & then
        assertThatThrownBy(() -> connect(99L, roomId, new CollectingHandler(objectMapper)))
                .hasCauseInstanceOf(jakarta.websocket.DeploymentException.class);
    }

    private WebSocketSession connect(
            final Long userId, final UUID roomId, final TextWebSocketHandler handler
    ) throws Exception {
        final StandardWebSocketClient client = new StandardWebSocketClient();
        final URI uri = URI.create("ws://localhost:%d/ws/signaling?userId=%d&roomId=%s"
                .formatted(port, userId, roomId));
        return client.execute(handler, null, uri).get(AWAIT_SECONDS, TimeUnit.SECONDS);
    }

    private void sendMessage(final WebSocketSession session, final SignalingMessage message) throws Exception {
        session.sendMessage(new TextMessage(objectMapper.writeValueAsString(message)));
    }

    private SignalingMessage offerOrAnswer(
            final SignalingMessageType type, final UUID roomId, final String sdp
    ) {
        final ObjectNode payload = objectMapper.createObjectNode().put("sdp", sdp);
        return new SignalingMessage(type, roomId, null, null, payload);
    }

    private static final class CollectingHandler extends TextWebSocketHandler {

        private final ObjectMapper objectMapper;
        private final BlockingQueue<SignalingMessage> received = new LinkedBlockingQueue<>();

        CollectingHandler(final ObjectMapper objectMapper) {
            this.objectMapper = objectMapper;
        }

        @Override
        protected void handleTextMessage(final WebSocketSession session, final TextMessage message) {
            received.offer(objectMapper.readValue(message.getPayload(), SignalingMessage.class));
        }

        SignalingMessage awaitNext() throws InterruptedException {
            final SignalingMessage message = received.poll(AWAIT_SECONDS, TimeUnit.SECONDS);
            assertThat(message).as("expected to receive a signaling message within %ds", AWAIT_SECONDS).isNotNull();
            return message;
        }
    }
}
