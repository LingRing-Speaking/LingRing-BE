package com.lingring.domain.userreport.dto.request;

import com.lingring.domain.userreport.domain.ReportReason;

public record UserReportCreateRequest(
        Long reportedUserId,
        ReportReason reason,
        String description
) {
}
