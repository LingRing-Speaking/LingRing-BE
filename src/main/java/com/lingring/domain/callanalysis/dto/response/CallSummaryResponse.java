package com.lingring.domain.callanalysis.dto.response;

import com.lingring.domain.call.dao.dto.CallSummaryProjection;
import com.lingring.domain.callanalysis.dto.response.CallAnalysisStatusView;
import com.lingring.domain.callanalysis.service.CallAnalysisSummary;
import com.lingring.global.util.Zones;
import java.time.OffsetDateTime;

public record CallSummaryResponse(
        Long id,
        PartnerResponse partner,
        OffsetDateTime startedAt,
        int durationSec,
        Long analysisId,
        CallAnalysisStatusView analysisStatus
) {

    public static CallSummaryResponse from(
            final CallSummaryProjection projection,
            final CallAnalysisSummary analysisSummary
    ) {
        final OffsetDateTime startedAt = projection.getStartedAt()
                .atZone(Zones.SEOUL)
                .toOffsetDateTime();
        return new CallSummaryResponse(
                projection.getId(),
                partnerOf(projection),
                startedAt,
                projection.getDurationSec().intValue(),
                analysisIdOf(analysisSummary),
                CallAnalysisStatusView.from(analysisSummary)
        );
    }

    private static Long analysisIdOf(final CallAnalysisSummary summary) {
        if (summary == null) {
            return null;
        }
        return summary.analysisId();
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