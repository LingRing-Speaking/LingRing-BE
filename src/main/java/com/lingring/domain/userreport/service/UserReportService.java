package com.lingring.domain.userreport.service;

import com.lingring.domain.userblock.dao.UserBlockRepository;
import com.lingring.domain.userblock.domain.UserBlock;
import com.lingring.domain.userreport.dao.UserReportRepository;
import com.lingring.domain.userreport.domain.UserReport;
import com.lingring.domain.userreport.dto.request.UserReportCreateRequest;
import com.lingring.domain.userreport.dto.response.UserReportResponse;
import com.lingring.global.error.ErrorCode;
import com.lingring.global.error.exception.BadRequestException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserReportService {

    private final UserReportRepository userReportRepository;
    private final UserBlockRepository userBlockRepository;

    @Transactional
    public UserReportResponse report(final Long userId, final UserReportCreateRequest request) {
        final Long reportedUserId = request.reportedUserId();
        validateNotSelfReport(userId, reportedUserId);

        final UserReport saved = userReportRepository.save(
                UserReport.create(userId, reportedUserId, request.reason(), request.description())
        );
        autoBlock(userId, reportedUserId);
        return UserReportResponse.from(saved);
    }

    @Transactional
    public void anonymizeReporter(final Long userId) {
        userReportRepository.anonymizeReporter(userId);
    }

    private void autoBlock(final Long userId, final Long reportedUserId) {
        if (userBlockRepository.existsByUserIdAndBlockedUserId(userId, reportedUserId)) {
            return;
        }
        userBlockRepository.save(UserBlock.create(userId, reportedUserId));
    }

    private void validateNotSelfReport(final Long userId, final Long reportedUserId) {
        if (userId.equals(reportedUserId)) {
            throw new BadRequestException(
                    ErrorCode.SELF_REPORT_NOT_ALLOWED,
                    "userId가 %d인 사용자가 자기 자신을 신고하려 했습니다.".formatted(userId)
            );
        }
    }
}
