package com.lingring.infrastructure.redis;

import com.lingring.domain.signaling.domain.SignalingMessage;
import com.lingring.domain.signaling.service.SignalingSessionMessenger;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Component
@RequiredArgsConstructor
@Slf4j
public class RedisSignalingSubscriber implements MessageListener {

    private final ObjectMapper objectMapper;
    private final SignalingSessionMessenger sessionMessenger;

    @Override
    public void onMessage(final Message message, final byte[] pattern) {
        final SignalingMessage parsed = parse(message.getBody());
        if (parsed == null) {
            return;
        }
        if (parsed.toUserId() == null) {
            return;
        }
        sessionMessenger.sendToUser(parsed.toUserId(), parsed);
    }

    private SignalingMessage parse(final byte[] body) {
        try {
            return objectMapper.readValue(body, SignalingMessage.class);
        } catch (final RuntimeException e) {
            log.warn("Failed to parse signaling message from Redis: {}", e.getMessage());
            return null;
        }
    }
}
