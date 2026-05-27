package com.lingring.global.config;

import com.lingring.domain.call.api.SignalingHandshakeInterceptor;
import com.lingring.domain.call.api.SignalingWebSocketHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
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
}
