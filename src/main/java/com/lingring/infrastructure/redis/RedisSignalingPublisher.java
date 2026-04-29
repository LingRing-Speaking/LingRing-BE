package com.lingring.infrastructure.redis;

import com.lingring.domain.signaling.domain.SignalingMessage;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Component
@RequiredArgsConstructor
public class RedisSignalingPublisher {

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public void publish(final UUID roomId, final SignalingMessage message) {
        final String channel = SignalingChannels.forRoom(roomId);
        final String json = objectMapper.writeValueAsString(message);
        redisTemplate.convertAndSend(channel, json);
    }
}
