package com.lingring.domain.call.domain.port;

import com.lingring.domain.call.domain.SignalingMessage;
import java.util.UUID;

public interface SignalingPublisher {

    void publish(UUID roomId, SignalingMessage message);
}