package com.lingring.domain.push.service;

import com.lingring.domain.push.dao.DeviceTokenRepository;
import com.lingring.domain.push.domain.DeviceToken;
import com.lingring.domain.push.domain.policy.DailyReminderPolicy;
import com.lingring.domain.push.domain.port.PushMessage;
import com.lingring.domain.push.domain.port.PushSendResult;
import com.lingring.domain.push.domain.port.PushSender;
import com.lingring.global.util.DateTimeProvider;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class PushSendService {

    private final DeviceTokenRepository deviceTokenRepository;
    private final PushSender pushSender;
    private final DateTimeProvider timeProvider;

    public void sendTest(final Long userId) {
        send(deviceTokenRepository.findAllByUserId(userId), PushMessage.dailyReminder());
    }

    public void sendDailyReminder() {
        final PushMessage message = PushMessage.dailyReminder();
        long lastId = 0L;
        while (DailyReminderPolicy.isSendable(timeProvider.now())) {
            final List<DeviceToken> targets = deviceTokenRepository.findDailyReminderTargets(
                    lastId,
                    Limit.of(DailyReminderPolicy.CHUNK_SIZE)
            );
            send(targets, message);
            if (targets.size() < DailyReminderPolicy.CHUNK_SIZE) {
                return;
            }
            lastId = targets.getLast().getId();
        }
        log.warn("데일리 리마인더 발송 가능 시간대를 벗어나 중단. lastId={}", lastId);
    }

    // 외부(FCM) 호출이 DB 트랜잭션·커넥션을 붙잡지 않도록 트랜잭션 없이 조회 → 발송 → 정리 순으로 처리한다
    private void send(final List<DeviceToken> deviceTokens, final PushMessage message) {
        if (deviceTokens.isEmpty()) {
            return;
        }
        final List<String> tokens = deviceTokens.stream()
                .map(DeviceToken::getToken)
                .toList();
        final PushSendResult result = pushSender.send(tokens, message);
        removeInvalidTokens(result);
    }

    private void removeInvalidTokens(final PushSendResult result) {
        if (!result.hasInvalidTokens()) {
            return;
        }
        final int deleted = deviceTokenRepository.deleteAllByTokenIn(result.invalidTokens());
        log.info("무효 디바이스 토큰 삭제. count={}", deleted);
    }
}
