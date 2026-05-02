package com.lingring.domain.call.dao.dto;

import java.time.LocalDateTime;

public interface CallSummaryProjection {

    Long getId();

    Long getPartnerId();

    String getPartnerName();

    LocalDateTime getStartedAt();

    LocalDateTime getEndedAt();
}
