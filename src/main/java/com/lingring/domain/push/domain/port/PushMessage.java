package com.lingring.domain.push.domain.port;

import com.lingring.domain.push.domain.PushType;

public record PushMessage(
        String title,
        String body,
        PushType type
) {

    // 광고성 정보(정보통신망법 제50조)는 제목 앞에 (광고)를, 본문에 수신거부 방법을 항상 명시해야 한다
    private static final String AD_PREFIX = "(광고) ";
    private static final String AD_UNSUBSCRIBE_GUIDE = "\n수신거부: 마이페이지 > 설정 > 알림 받기";
    private static final String DAILY_REMINDER_TITLE = "링링 통화 오픈! (20:00~23:00)";
    private static final String DAILY_REMINDER_BODY = "오늘 하루 어땠는지 영어로 얘기해 볼까요?";

    public static PushMessage dailyReminder() {
        return advertisement(DAILY_REMINDER_TITLE, DAILY_REMINDER_BODY, PushType.DAILY_REMINDER);
    }

    private static PushMessage advertisement(final String title, final String body, final PushType type) {
        return new PushMessage(AD_PREFIX + title, body + AD_UNSUBSCRIBE_GUIDE, type);
    }
}
