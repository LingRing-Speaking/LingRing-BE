package com.lingring.domain.push.domain.policy;

import static lombok.AccessLevel.PRIVATE;

import java.time.LocalDateTime;
import java.time.LocalTime;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = PRIVATE)
public final class DailyReminderPolicy {

    public static final int CHUNK_SIZE = 500;

    private static final LocalTime SEND_START = LocalTime.of(20, 0);
    // 21시~다음 날 8시 광고성 정보 발송은 별도 야간 동의가 필요하므로 지연·재시도로도 21시를 넘기지 않는다
    private static final LocalTime SEND_DEADLINE = LocalTime.of(21, 0);

    public static boolean isSendable(final LocalDateTime now) {
        final LocalTime time = now.toLocalTime();
        return !time.isBefore(SEND_START) && time.isBefore(SEND_DEADLINE);
    }
}
