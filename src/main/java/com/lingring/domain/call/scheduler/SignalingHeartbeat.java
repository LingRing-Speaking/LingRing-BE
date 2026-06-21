package com.lingring.domain.call.scheduler;

import com.lingring.domain.call.dao.LocalSessionRegistry;
import java.io.IOException;
import java.util.concurrent.Executor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.PingMessage;
import org.springframework.web.socket.WebSocketSession;

@Component
@Slf4j
public class SignalingHeartbeat {

    private final LocalSessionRegistry sessionRegistry;
    private final Executor signalingHeartbeatExecutor;

    public SignalingHeartbeat(
            final LocalSessionRegistry sessionRegistry,
            @Qualifier("signalingHeartbeatExecutor") final Executor signalingHeartbeatExecutor
    ) {
        this.sessionRegistry = sessionRegistry;
        this.signalingHeartbeatExecutor = signalingHeartbeatExecutor;
    }

    /**
     * 유휴 시그널링 WS가 ALB idle timeout에 끊기지 않도록 WebSocket ping 프레임을 주기 전송한다.
     * LocalSessionRegistry는 인스턴스별 로컬 세션이므로 @SchedulerLock을 붙이지 않는다 —
     * 모든 인스턴스가 각자 보유한 세션에 핑을 보내야 한다.
     * 전송은 전용 executor에 분산해, 블로킹된 한 세션이 나머지 세션의 핑을 막지 않게 한다.
     */
    @Scheduled(fixedDelayString = "${signaling.heartbeat-interval:25s}")
    public void sendPings() {
        final PingMessage ping = new PingMessage();
        for (final WebSocketSession session : sessionRegistry.activeSessions()) {
            signalingHeartbeatExecutor.execute(() -> ping(session, ping));
        }
    }

    private void ping(final WebSocketSession session, final PingMessage ping) {
        if (!session.isOpen()) {
            return;
        }
        try {
            session.sendMessage(ping);
        } catch (final IOException e) {
            log.warn("Failed to ping signaling session {}: {}", session.getId(), e.getMessage());
        }
    }
}
