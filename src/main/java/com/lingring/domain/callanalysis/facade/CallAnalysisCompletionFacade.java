package com.lingring.domain.callanalysis.facade;

import com.lingring.domain.callanalysis.service.CallTranscriptService;
import com.lingring.domain.callanalysis.domain.vo.AnalysisResult;
import com.lingring.domain.callanalysis.domain.vo.FeedbackTag;
import com.lingring.domain.callanalysis.domain.vo.MistakeItem;
import com.lingring.domain.callanalysis.domain.vo.Mistakes;
import com.lingring.domain.callanalysis.domain.vo.PositiveItem;
import com.lingring.domain.callanalysis.domain.vo.Positives;
import com.lingring.domain.callanalysis.service.CallAnalysisService;
import com.lingring.infrastructure.sqs.CallAnalysisResultMessage;
import com.lingring.infrastructure.sqs.CallAnalysisResultMessage.MistakeItemPart;
import com.lingring.infrastructure.sqs.CallAnalysisResultMessage.PositiveItemPart;
import com.lingring.infrastructure.sqs.CallAnalysisResultMessage.UserAnalysisPart;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class CallAnalysisCompletionFacade {

    private final CallTranscriptService callTranscriptService;
    private final CallAnalysisService callAnalysisService;

    @Transactional
    public void complete(final CallAnalysisResultMessage message) {
        callTranscriptService.complete(message.callId(), message.transcript().segments());
        for (final UserAnalysisPart userAnalysis : message.analyses()) {
            callAnalysisService.complete(
                    message.callId(),
                    userAnalysis.userId(),
                    toAnalysisResult(userAnalysis),
                    message.modelIdentifier()
            );
        }
    }

    private AnalysisResult toAnalysisResult(final UserAnalysisPart userAnalysis) {
        return new AnalysisResult(
                new Mistakes(userAnalysis.mistakes().stream()
                        .map(this::toMistakeItem)
                        .toList()
                ),
                new Positives(userAnalysis.positives().stream()
                        .map(this::toPositiveItem)
                        .toList()
                )
        );
    }

    private MistakeItem toMistakeItem(final MistakeItemPart part) {
        return new MistakeItem(
                FeedbackTag.valueOf(part.tag()),
                part.wrong(),
                part.improved(),
                part.reason(),
                part.koMeaning()
        );
    }

    private PositiveItem toPositiveItem(final PositiveItemPart part) {
        return new PositiveItem(
                part.sentence(),
                part.goodPart(),
                part.koMeaning()
        );
    }
}
