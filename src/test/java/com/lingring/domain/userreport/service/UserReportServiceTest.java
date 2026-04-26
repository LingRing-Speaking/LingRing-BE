package com.lingring.domain.userreport.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.lingring.domain.userblock.dao.UserBlockRepository;
import com.lingring.domain.userblock.domain.UserBlock;
import com.lingring.domain.userreport.dao.UserReportRepository;
import com.lingring.domain.userreport.domain.ReportReason;
import com.lingring.domain.userreport.dto.request.UserReportCreateRequest;
import com.lingring.domain.userreport.dto.response.UserReportResponse;
import com.lingring.global.config.ServiceIntegrationHelper;
import com.lingring.global.error.ErrorCode;
import com.lingring.global.error.exception.BadRequestException;
import com.lingring.global.error.exception.InvalidValueException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class UserReportServiceTest extends ServiceIntegrationHelper {

    @Autowired
    private UserReportService userReportService;

    @Autowired
    private UserReportRepository userReportRepository;

    @Autowired
    private UserBlockRepository userBlockRepository;

    @Nested
    @DisplayName("report: 사용자 신고")
    class Report {

        @Test
        @DisplayName("정상 신고 시 신고 row와 차단 row가 모두 생성된다")
        void report_whenValid_createsReportAndBlock() {
            // given
            final Long userId = 1L;
            final UserReportCreateRequest request = new UserReportCreateRequest(
                    2L, ReportReason.INAPPROPRIATE_CONVERSATION, "도배성 메시지를 반복적으로 보냄"
            );

            // when
            final UserReportResponse response = userReportService.report(userId, request);

            // then
            assertThat(response.id()).isNotNull();
            assertThat(response.userId()).isEqualTo(userId);
            assertThat(response.reportedUserId()).isEqualTo(2L);
            assertThat(response.reason()).isEqualTo(ReportReason.INAPPROPRIATE_CONVERSATION);
            assertThat(response.reasonLabel()).isEqualTo("부적절한 대화");
            assertThat(response.description()).isEqualTo("도배성 메시지를 반복적으로 보냄");
            assertThat(userReportRepository.findById(response.id())).isPresent();
            assertThat(userBlockRepository.existsByUserIdAndBlockedUserId(userId, 2L)).isTrue();
        }

        @Test
        @DisplayName("description이 5자 미만이면 INVALID_REPORT_DESCRIPTION 예외가 발생한다")
        void report_whenDescriptionTooShort_throwsInvalidValue() {
            // given
            final UserReportCreateRequest request = new UserReportCreateRequest(
                    2L, ReportReason.OTHER, "ABC"
            );

            // when & then
            assertThatThrownBy(() -> userReportService.report(1L, request))
                    .isInstanceOf(InvalidValueException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.INVALID_REPORT_DESCRIPTION);
        }

        @Test
        @DisplayName("자기 자신을 신고하면 SELF_REPORT_NOT_ALLOWED 예외가 발생한다")
        void report_whenSelfReport_throwsBadRequest() {
            // given
            final Long userId = 1L;
            final UserReportCreateRequest request = new UserReportCreateRequest(
                    userId, ReportReason.BAD_MANNERS, "자기 자신을 신고하는 시도"
            );

            // when & then
            assertThatThrownBy(() -> userReportService.report(userId, request))
                    .isInstanceOf(BadRequestException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.SELF_REPORT_NOT_ALLOWED);
        }

        @Test
        @DisplayName("같은 대상을 두 번 신고하면 신고 row 2건, 차단 row 1건만 유지된다")
        void report_whenReportTwice_createsTwoReportsButOneBlock() {
            // given
            final Long userId = 1L;
            final UserReportCreateRequest first = new UserReportCreateRequest(
                    2L, ReportReason.INAPPROPRIATE_CONVERSATION, "첫 번째 신고 내용입니다"
            );
            final UserReportCreateRequest second = new UserReportCreateRequest(
                    2L, ReportReason.BAD_MANNERS, "두 번째 신고 내용입니다"
            );

            // when
            userReportService.report(userId, first);
            userReportService.report(userId, second);

            // then
            assertThat(userReportRepository.findAll()).hasSize(2);
            assertThat(userBlockRepository.findAll()).hasSize(1);
        }

        @Test
        @DisplayName("이미 수동 차단된 대상을 신고해도 차단 row는 추가되지 않는다")
        void report_whenAlreadyBlocked_doesNotDuplicateBlock() {
            // given
            final Long userId = 1L;
            userBlockRepository.save(UserBlock.create(userId, 2L));
            final UserReportCreateRequest request = new UserReportCreateRequest(
                    2L, ReportReason.BAD_MANNERS, "이미 차단되어 있는 대상 신고"
            );

            // when
            userReportService.report(userId, request);

            // then
            assertThat(userReportRepository.findAll()).hasSize(1);
            assertThat(userBlockRepository.findAll()).hasSize(1);
        }
    }
}
