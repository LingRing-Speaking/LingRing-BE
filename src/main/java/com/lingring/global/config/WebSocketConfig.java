package com.lingring.global.config;

import com.lingring.domain.call.api.SignalingHandshakeInterceptor;
import com.lingring.domain.call.api.SignalingWebSocketHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketConfigurer {

    private static final String SIGNALING_ENDPOINT = "/ws/signaling";

    private static final String[] ALLOWED_ORIGIN_PATTERNS = {
            "http://localhost:5173",
            "http://localhost:4173",
            "capacitor://*",
            "https://lingring.site",
            "https://dev-lingring.site",
            "http://dev-lingring.site"
    };

    private final SignalingWebSocketHandler signalingWebSocketHandler;
    private final SignalingHandshakeInterceptor signalingHandshakeInterceptor;

    @Override
    public void registerWebSocketHandlers(final WebSocketHandlerRegistry registry) {
        registry.addHandler(signalingWebSocketHandler, SIGNALING_ENDPOINT)
                .addInterceptors(signalingHandshakeInterceptor)
                .setAllowedOriginPatterns(ALLOWED_ORIGIN_PATTERNS);
    }

    @Bean
    public ThreadPoolTaskExecutor signalingHeartbeatExecutor() {
        // 핑 전송을 세션별로 분산해, 블로킹된 한 세션이 나머지 핑을 막지 않게 한다.
        // 스케줄링이 꺼져도(@Scheduled 미발화) SignalingHeartbeat 빈은 항상 로드되므로
        // executor는 scheduling 조건과 무관하게 항상 존재해야 한다 (그래서 SchedulingConfig가 아닌 여기).
        final ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(3);
        executor.setMaxPoolSize(3);
        executor.setQueueCapacity(1000);
        executor.setThreadNamePrefix("ws-heartbeat-");
        return executor;
    }
}
