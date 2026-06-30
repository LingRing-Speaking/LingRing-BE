package com.lingring.domain.review.domain.recording.policy;

import com.lingring.domain.review.exception.CallRecordingExpiredException;
import com.lingring.global.config.CallRecordingRetentionProperties;
import com.lingring.global.util.DateTimeProvider;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 통화 종료 시각 기준 녹음 보유 기간(기본 30일) 경과 여부를 판정한다.
 * 기간이 지나면 녹음이 파기되어 분석을 시작/재시도할 수 없다.
 */
@Component
@RequiredArgsConstructor
public class CallRecordingRetentionPolicy {

    private final CallRecordingRetentionProperties properties;
    private final DateTimeProvider timeProvider;

    public boolean isExpired(final LocalDateTime callEndedAt) {
        final LocalDateTime threshold = timeProvider.now().minusDays(properties.retentionDays());
        return callEndedAt.isBefore(threshold);
    }

    public void requireNotExpired(final Long callId, final LocalDateTime callEndedAt) {
        if (isExpired(callEndedAt)) {
            throw new CallRecordingExpiredException(callId);
        }
    }
}
