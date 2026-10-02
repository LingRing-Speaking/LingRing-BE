package com.lingring.domain.push.domain.port;

import java.util.List;

public interface PushSender {

    PushSendResult send(List<String> tokens, PushMessage message);
}
