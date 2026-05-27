package com.lingring.infrastructure.sqs;

import com.lingring.domain.review.domain.transcript.vo.TranscriptSegment;
import java.util.List;

public record CallAnalysisResultMessage(
        Long callId,
        String modelIdentifier,
        TranscriptPart transcript,
        List<UserAnalysisPart> analyses
) {

    public record TranscriptPart(List<TranscriptSegment> segments) {
    }

    public record UserAnalysisPart(
            Long userId,
            List<MistakeItemPart> mistakes,
            List<PositiveItemPart> positives
    ) {
    }

    public record MistakeItemPart(
            String tag,
            String wrong,
            String improved,
            String reason,
            String koMeaning
    ) {
    }

    public record PositiveItemPart(
            String sentence,
            String goodPart,
            String koMeaning
    ) {
    }
}
