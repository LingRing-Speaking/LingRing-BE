package com.lingring.domain.review.dto.response;

import java.time.LocalDateTime;

public record AnalysisQuotaResponse(
        int freeTicket,
        int paidTicket,
        LocalDateTime nextResetAt
) {
}
