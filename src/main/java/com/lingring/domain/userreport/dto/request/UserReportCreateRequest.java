package com.lingring.domain.userreport.dto.request;

import com.lingring.domain.userreport.domain.ReportReason;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record UserReportCreateRequest(
        @NotNull(message = "reportedUserId가 비어있습니다.")
        @Positive(message = "reportedUserId는 양수여야 합니다.")
        Long reportedUserId,
        @NotNull(message = "reason이 비어있습니다.")
        ReportReason reason,
        @NotBlank(message = "description이 비어있습니다.")
        @Size(min = 5, max = 200, message = "description은 5자 이상 200자 이하여야 합니다.")
        String description
) {
}
