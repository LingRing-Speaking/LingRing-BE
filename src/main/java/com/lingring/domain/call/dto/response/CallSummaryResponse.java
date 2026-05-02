package com.lingring.domain.call.dto.response;

import com.lingring.domain.call.dao.dto.CallSummaryProjection;
import com.lingring.global.util.Zones;
import java.time.Duration;
import java.time.OffsetDateTime;

public record CallSummaryResponse(
        Long id,
        PartnerResponse partner,
        OffsetDateTime startedAt,
        int durationSec,
        boolean analyzed
) {

    public static CallSummaryResponse from(final CallSummaryProjection projection) {
        final OffsetDateTime startedAt = projection.getStartedAt()
                .atZone(Zones.SEOUL)
                .toOffsetDateTime();
        final int durationSec = (int) Duration.between(
                projection.getStartedAt(),
                projection.getEndedAt()
        ).toSeconds();
        // TODO: call_analyze 도메인 추가 후 실제 분석 여부로 교체
        final boolean analyzed = false;
        return new CallSummaryResponse(
                projection.getId(),
                new PartnerResponse(projection.getPartnerId(), projection.getPartnerName()),
                startedAt,
                durationSec,
                analyzed
        );
    }
}
