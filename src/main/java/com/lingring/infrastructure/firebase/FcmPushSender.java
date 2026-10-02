package com.lingring.infrastructure.firebase;

import com.google.firebase.messaging.BatchResponse;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.MessagingErrorCode;
import com.google.firebase.messaging.Notification;
import com.google.firebase.messaging.SendResponse;
import com.lingring.domain.push.domain.port.PushMessage;
import com.lingring.domain.push.domain.port.PushSendResult;
import com.lingring.domain.push.domain.port.PushSender;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class FcmPushSender implements PushSender {

    // FCM sendEach 한 번에 보낼 수 있는 최대 메시지 수
    private static final int MAX_BATCH_SIZE = 500;
    private static final String DATA_TYPE_KEY = "type";
    // INVALID_ARGUMENT는 payload 오류에도 나오므로 제외한다 (오판 시 전체 토큰 삭제 위험)
    private static final Set<MessagingErrorCode> INVALID_TOKEN_ERRORS = EnumSet.of(
            MessagingErrorCode.UNREGISTERED,
            MessagingErrorCode.SENDER_ID_MISMATCH
    );

    private final FirebaseMessaging firebaseMessaging;

    @Override
    public PushSendResult send(final List<String> tokens, final PushMessage message) {
        final List<String> invalidTokens = new ArrayList<>();
        for (int from = 0; from < tokens.size(); from += MAX_BATCH_SIZE) {
            final List<String> batch = tokens.subList(from, Math.min(from + MAX_BATCH_SIZE, tokens.size()));
            invalidTokens.addAll(sendBatch(batch, message));
        }
        return PushSendResult.withInvalidTokens(invalidTokens);
    }

    private List<String> sendBatch(final List<String> tokens, final PushMessage message) {
        final List<Message> messages = tokens.stream()
                .map(token -> toMessage(token, message))
                .toList();
        try {
            final BatchResponse response = firebaseMessaging.sendEach(messages);
            log.info("FCM 발송 완료. type={}, success={}, failure={}",
                    message.type(), response.getSuccessCount(), response.getFailureCount());
            return collectInvalidTokens(tokens, response.getResponses());
        } catch (final FirebaseMessagingException e) {
            log.error("FCM 일괄 발송 실패. type={}, size={}", message.type(), tokens.size(), e);
            return List.of();
        }
    }

    private Message toMessage(final String token, final PushMessage message) {
        return Message.builder()
                .setToken(token)
                .setNotification(Notification.builder()
                        .setTitle(message.title())
                        .setBody(message.body())
                        .build())
                .putData(DATA_TYPE_KEY, message.type().name())
                .build();
    }

    private List<String> collectInvalidTokens(final List<String> tokens, final List<SendResponse> responses) {
        final List<String> invalidTokens = new ArrayList<>();
        for (int i = 0; i < responses.size(); i++) {
            final SendResponse response = responses.get(i);
            if (response.isSuccessful()) {
                continue;
            }
            final MessagingErrorCode errorCode = response.getException().getMessagingErrorCode();
            if (INVALID_TOKEN_ERRORS.contains(errorCode)) {
                invalidTokens.add(tokens.get(i));
                continue;
            }
            log.warn("FCM 개별 발송 실패. errorCode={}", errorCode, response.getException());
        }
        return invalidTokens;
    }
}
