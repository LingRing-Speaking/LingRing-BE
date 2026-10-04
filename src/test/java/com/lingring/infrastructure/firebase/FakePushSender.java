package com.lingring.infrastructure.firebase;

import com.lingring.domain.push.domain.port.PushMessage;
import com.lingring.domain.push.domain.port.PushSendResult;
import com.lingring.domain.push.domain.port.PushSender;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class FakePushSender implements PushSender {

    private final Set<String> invalidTokens = new HashSet<>();
    private final List<SentPush> sent = new ArrayList<>();

    @Override
    public PushSendResult send(final List<String> tokens, final PushMessage message) {
        tokens.forEach(token -> sent.add(new SentPush(token, message)));
        return PushSendResult.withInvalidTokens(tokens.stream()
                .filter(invalidTokens::contains)
                .toList());
    }

    public void markInvalid(final String token) {
        invalidTokens.add(token);
    }

    public List<SentPush> sent() {
        return List.copyOf(sent);
    }

    public List<String> sentTokens() {
        return sent.stream()
                .map(SentPush::token)
                .toList();
    }

    public void clear() {
        invalidTokens.clear();
        sent.clear();
    }

    public record SentPush(
            String token,
            PushMessage message
    ) {
    }
}
