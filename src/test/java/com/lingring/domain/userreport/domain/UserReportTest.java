package com.lingring.domain.userreport.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.lingring.global.error.ErrorCode;
import com.lingring.global.error.exception.InvalidValueException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class UserReportTest {

    @Test
    @DisplayName("create는 모든 필드를 보유한 엔티티를 생성한다")
    void createWrapsValuesIntoEntity() {
        // given
        final Long userId = 1L;
        final Long reportedUserId = 2L;
        final ReportReason reason = ReportReason.INAPPROPRIATE_CONVERSATION;
        final String description = "도배성 메시지를 반복적으로 보냄";

        // when
        final UserReport userReport = UserReport.create(userId, reportedUserId, reason, description);

        // then
        assertThat(userReport.getUserId()).isEqualTo(userId);
        assertThat(userReport.getReportedUserId()).isEqualTo(reportedUserId);
        assertThat(userReport.getReason()).isEqualTo(reason);
        assertThat(userReport.getDescription().getValue()).isEqualTo(description);
    }

    @Test
    @DisplayName("description 검증이 자동으로 실행된다 (5자 미만 거부)")
    void createValidatesDescription() {
        assertThatThrownBy(() -> UserReport.create(1L, 2L, ReportReason.OTHER, "ABC"))
                .isInstanceOf(InvalidValueException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_REPORT_DESCRIPTION);
    }
}
