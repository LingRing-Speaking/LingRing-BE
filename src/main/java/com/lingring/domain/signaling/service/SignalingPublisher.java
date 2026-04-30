package com.lingring.domain.signaling.service;

import com.lingring.domain.signaling.domain.SignalingMessage;
import java.util.UUID;

public interface SignalingPublisher {

    void publish(UUID roomId, SignalingMessage message);
}