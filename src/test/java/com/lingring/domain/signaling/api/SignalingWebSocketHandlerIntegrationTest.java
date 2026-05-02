package com.lingring.domain.signaling.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.lingring.domain.call.dao.CallHistoryRepository;
import com.lingring.domain.call.domain.CallHistory;
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
    private CallHistoryRepository callHistoryRepository;

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
        callHistoryRepository.save(CallHistory.start(1L, 2L, roomId, STARTED_AT));

        final CollectingHandler handlerA = new CollectingHandler(objectMapper);
        final CollectingHandler handlerB = new CollectingHandler(objectMapper);

        final WebSocketSession sessionA = connect(1L, roomId, handlerA);
        final WebSocketSession sessionB = connect(2L, roomId, handlerB);

        // when: 양쪽 JOIN
        sendMessage(sessionA, new SignalingMessage(SignalingMessageType.JOIN, null, null, null));
        sendMessage(sessionB, new SignalingMessage(SignalingMessageType.JOIN, null, null, null));

        // then: 양쪽이 READY 수신, caller=1L, callee=2L
        final SignalingMessage readyForA = handlerA.awaitNext();
        final SignalingMessage readyForB = handlerB.awaitNext();
        assertThat(readyForA.type()).isEqualTo(SignalingMessageType.READY);
        assertThat(readyForB.type()).isEqualTo(SignalingMessageType.READY);
        assertThat(readyForA.payload().get("callerUserId").asLong()).isEqualTo(1L);
        assertThat(readyForA.payload().get("calleeUserId").asLong()).isEqualTo(2L);

        // OFFER A → B
        sendMessage(sessionA, offerOrAnswer(SignalingMessageType.OFFER, "offer-sdp"));
        final SignalingMessage offerForB = handlerB.awaitNext();
        assertThat(offerForB.type()).isEqualTo(SignalingMessageType.OFFER);
        assertThat(offerForB.fromUserId()).isEqualTo(1L);
        assertThat(offerForB.toUserId()).isEqualTo(2L);
        assertThat(offerForB.payload().get("sdp").asString()).isEqualTo("offer-sdp");

        // ANSWER B → A
        sendMessage(sessionB, offerOrAnswer(SignalingMessageType.ANSWER, "answer-sdp"));
        final SignalingMessage answerForA = handlerA.awaitNext();
        assertThat(answerForA.type()).isEqualTo(SignalingMessageType.ANSWER);
        assertThat(answerForA.fromUserId()).isEqualTo(2L);
        assertThat(answerForA.toUserId()).isEqualTo(1L);

        // HANGUP A → B
        sendMessage(sessionA, new SignalingMessage(SignalingMessageType.HANGUP, null, null, null));
        final SignalingMessage hangupForB = handlerB.awaitNext();
        assertThat(hangupForB.type()).isEqualTo(SignalingMessageType.HANGUP);

        // CallHistory가 종료 상태로 전이됨
        Awaitility.await().atMost(Duration.ofSeconds(AWAIT_SECONDS))
                .untilAsserted(() -> assertThat(callHistoryRepository.findByRoomId(roomId).orElseThrow().isActive())
                        .isFalse());

        sessionA.close();
        sessionB.close();
    }

    @Test
    @DisplayName("일방적 disconnect 시 상대에게 HANGUP이 전달되고 CallHistory가 종료된다")
    void disconnect_propagatesHangupAndEndsCall() throws Exception {
        // given
        final UUID roomId = UUID.randomUUID();
        callHistoryRepository.save(CallHistory.start(1L, 2L, roomId, STARTED_AT));

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
                .untilAsserted(() -> assertThat(callHistoryRepository.findByRoomId(roomId).orElseThrow().isActive())
                        .isFalse());

        sessionB.close();
    }

    @Test
    @DisplayName("grace period 안에 재연결하면 통화가 종료되지 않는다")
    void reconnectWithinGracePeriod_keepsCallAlive() throws Exception {
        // given: 통화 생성 후 user 1이 연결, JOIN까지 진행해 set에 진입
        final UUID roomId = UUID.randomUUID();
        callHistoryRepository.save(CallHistory.start(1L, 2L, roomId, STARTED_AT));

        final CollectingHandler handlerA1 = new CollectingHandler(objectMapper);
        final CollectingHandler handlerA2 = new CollectingHandler(objectMapper);
        final CollectingHandler handlerB = new CollectingHandler(objectMapper);

        final WebSocketSession sessionA1 = connect(1L, roomId, handlerA1);
        final WebSocketSession sessionB = connect(2L, roomId, handlerB);
        sendMessage(sessionA1, new SignalingMessage(SignalingMessageType.JOIN, null, null, null));

        // when: A가 끊기지만 grace 안에 재연결, 그 사이 B는 JOIN
        sessionA1.close();
        Awaitility.await().atMost(Duration.ofSeconds(AWAIT_SECONDS))
                .until(() -> !sessionA1.isOpen());

        final WebSocketSession sessionA2 = connect(1L, roomId, handlerA2);
        sendMessage(sessionB, new SignalingMessage(SignalingMessageType.JOIN, null, null, null));

        // then: 통화는 살아 있고 READY가 양쪽에 도달
        final SignalingMessage readyForA2 = handlerA2.awaitNext();
        final SignalingMessage readyForB = handlerB.awaitNext();
        assertThat(readyForA2.type()).isEqualTo(SignalingMessageType.READY);
        assertThat(readyForB.type()).isEqualTo(SignalingMessageType.READY);

        // grace 만료 시간을 넘겨도 통화가 종료되지 않아야 함
        Thread.sleep(1500);
        assertThat(callHistoryRepository.findByRoomId(roomId).orElseThrow().isActive())
                .isTrue();

        sessionA2.close();
        sessionB.close();
    }

    @Test
    @DisplayName("같은 user가 WS를 두 번 열어도 첫 세션 close가 통화를 종료시키지 않고 READY가 정상 발행된다")
    void duplicateSessionDoesNotTriggerCleanup() throws Exception {
        // given
        final UUID roomId = UUID.randomUUID();
        callHistoryRepository.save(CallHistory.start(1L, 2L, roomId, STARTED_AT));

        final CollectingHandler handlerA1 = new CollectingHandler(objectMapper);
        final CollectingHandler handlerA2 = new CollectingHandler(objectMapper);
        final CollectingHandler handlerB = new CollectingHandler(objectMapper);

        // when: user 1이 동일한 roomId로 두 세션을 연 뒤 살아남은 세션과 user 2가 JOIN
        final WebSocketSession sessionA1 = connect(1L, roomId, handlerA1);
        final WebSocketSession sessionA2 = connect(1L, roomId, handlerA2);
        final WebSocketSession sessionB = connect(2L, roomId, handlerB);

        // 첫 세션은 서버에 의해 close 되어야 함
        Awaitility.await().atMost(Duration.ofSeconds(AWAIT_SECONDS))
                .until(() -> !sessionA1.isOpen());

        sendMessage(sessionA2, new SignalingMessage(SignalingMessageType.JOIN, null, null, null));
        sendMessage(sessionB, new SignalingMessage(SignalingMessageType.JOIN, null, null, null));

        // then: 살아남은 sessionA2와 sessionB 모두 READY 수신 (cleanupRoom이 발화하지 않았음을 의미)
        final SignalingMessage readyForA2 = handlerA2.awaitNext();
        final SignalingMessage readyForB = handlerB.awaitNext();
        assertThat(readyForA2.type()).isEqualTo(SignalingMessageType.READY);
        assertThat(readyForB.type()).isEqualTo(SignalingMessageType.READY);

        // 통화는 종료되지 않아야 함
        assertThat(callHistoryRepository.findByRoomId(roomId).orElseThrow().isActive())
                .isTrue();

        sessionA2.close();
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
    @DisplayName("통화 비참여자가 연결을 시도하면 핸드셰이크가 실패한다")
    void connect_whenNotParticipant_handshakeFails() {
        // given
        final UUID roomId = UUID.randomUUID();
        callHistoryRepository.save(CallHistory.start(1L, 2L, roomId, STARTED_AT));

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

    private SignalingMessage offerOrAnswer(final SignalingMessageType type, final String sdp) {
        final ObjectNode payload = objectMapper.createObjectNode().put("sdp", sdp);
        return new SignalingMessage(type, null, null, payload);
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
