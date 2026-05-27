package com.lingring.domain.moderation.dto.response;

import com.lingring.domain.moderation.domain.ReportReason;
import com.lingring.domain.moderation.domain.UserReport;
import java.time.LocalDateTime;

public record UserReportResponse(
        Long id,
        Long userId,
        Long reportedUserId,
        ReportReason reason,
        String reasonLabel,
        String description,
        LocalDateTime createdAt
) {

    public static UserReportResponse from(final UserReport userReport) {
        return new UserReportResponse(
                userReport.getId(),
                userReport.getUserId(),
                userReport.getReportedUserId(),
                userReport.getReason(),
                userReport.getReason().getDescription(),
                userReport.getDescription().getValue(),
                userReport.getCreatedAt()
        );
    }
}
