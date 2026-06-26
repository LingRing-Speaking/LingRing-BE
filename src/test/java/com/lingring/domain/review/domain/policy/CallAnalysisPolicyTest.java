package com.lingring.domain.review.domain.policy;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.lingring.domain.review.exception.CallTooShortException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class CallAnalysisPolicyTest {

    private static final Long CALL_ID = 1L;

    private final CallAnalysisPolicy callAnalysisPolicy = new CallAnalysisPolicy();

    @Test
    @DisplayName("통화 시간이 60초 이상이면 예외를 던지지 않는다")
    void requireEnoughToAnalyze_whenAtLeastMinDuration_doesNotThrow() {
        // when & then
        assertThatCode(() -> callAnalysisPolicy.requireEnoughToAnalyze(CALL_ID, 60L))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("통화 시간이 60초 미만이면 CallTooShortException을 던진다")
    void requireEnoughToAnalyze_whenBelowMinDuration_throws() {
        // when & then
        assertThatThrownBy(() -> callAnalysisPolicy.requireEnoughToAnalyze(CALL_ID, 59L))
                .isInstanceOf(CallTooShortException.class);
    }

    @Test
    @DisplayName("durationSec이 null이면(미종료) CallTooShortException을 던진다")
    void requireEnoughToAnalyze_whenNull_throws() {
        // when & then
        assertThatThrownBy(() -> callAnalysisPolicy.requireEnoughToAnalyze(CALL_ID, null))
                .isInstanceOf(CallTooShortException.class);
    }
}
