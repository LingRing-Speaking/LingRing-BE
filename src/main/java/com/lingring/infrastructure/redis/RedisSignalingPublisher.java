package com.lingring.infrastructure.redis;

import com.lingring.domain.signaling.domain.SignalingMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Component
@RequiredArgsConstructor
public class RedisSignalingPublisher {

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public void publish(final SignalingMessage message) {
        final String channel = SignalingChannels.forRoom(message.roomId());
        final String json = objectMapper.writeValueAsString(message);
        redisTemplate.convertAndSend(channel, json);
    }
}
