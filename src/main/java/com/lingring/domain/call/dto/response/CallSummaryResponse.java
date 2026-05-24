package com.lingring.domain.call.dto.response;

import com.lingring.domain.call.dao.dto.CallSummaryProjection;
import com.lingring.global.util.Zones;
import java.time.OffsetDateTime;

public record CallSummaryResponse(
        Long id,
        PartnerResponse partner,
        OffsetDateTime startedAt,
        int durationSec,
        Long analysisId
) {

    public static CallSummaryResponse from(final CallSummaryProjection projection, final Long analysisId) {
        final OffsetDateTime startedAt = projection.getStartedAt()
                .atZone(Zones.SEOUL)
                .toOffsetDateTime();
        return new CallSummaryResponse(
                projection.getId(),
                partnerOf(projection),
                startedAt,
                projection.getDurationSec().intValue(),
                analysisId
        );
    }

    private static PartnerResponse partnerOf(final CallSummaryProjection projection) {
        if (projection.getPartnerId() == null) {
            return null;
        }
        return new PartnerResponse(
                projection.getPartnerId(),
                projection.getPartnerName(),
                projection.getPartnerProfileImage()
        );
    }
}
