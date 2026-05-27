package com.lingring.infrastructure.redis;

import com.lingring.domain.call.domain.SignalingMessage;
import com.lingring.domain.call.service.SignalingPublisher;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Component
@RequiredArgsConstructor
public class RedisSignalingPublisher implements SignalingPublisher {

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    @Override
    public void publish(final UUID roomId, final SignalingMessage message) {
        final String channel = SignalingChannels.forRoom(roomId);
        final String json = objectMapper.writeValueAsString(message);
        redisTemplate.convertAndSend(channel, json);
    }
}
