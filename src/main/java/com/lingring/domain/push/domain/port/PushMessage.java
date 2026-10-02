package com.lingring.domain.push.domain.port;

import com.lingring.domain.push.domain.PushType;

public record PushMessage(
        String title,
        String body,
        PushType type
) {

    // 광고성 정보(정보통신망법 제50조)로 보고 제목 앞에 항상 (광고)를 붙인다
    private static final String AD_PREFIX = "(광고) ";
    private static final String DAILY_REMINDER_TITLE = "링링 통화 오픈! (20:00~23:00)";
    private static final String DAILY_REMINDER_BODY = "오늘 하루 어땠는지 영어로 얘기해 볼까요?";

    public static PushMessage dailyReminder() {
        return new PushMessage(AD_PREFIX + DAILY_REMINDER_TITLE, DAILY_REMINDER_BODY, PushType.DAILY_REMINDER);
    }
}
