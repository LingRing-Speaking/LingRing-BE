package com.lingring.domain.push.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.lingring.domain.push.domain.port.PushMessage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class PushMessageTest {

    @Test
    @DisplayName("dailyReminder: 제목 앞에 (광고)를 붙이고 data.type은 DAILY_REMINDER다")
    void dailyReminder_hasAdPrefixAndType() {
        // when
        final PushMessage message = PushMessage.dailyReminder();

        // then
        assertThat(message.title()).isEqualTo("(광고) 링링 통화 오픈! (20:00~23:00)");
        assertThat(message.body()).isEqualTo("오늘 하루 어땠는지 영어로 얘기해 볼까요?");
        assertThat(message.type()).isEqualTo(PushType.DAILY_REMINDER);
    }
}
