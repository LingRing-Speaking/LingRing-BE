package com.lingring.domain.callanalysis.facade;

import static org.assertj.core.api.Assertions.assertThat;

import com.lingring.domain.callanalysis.dao.CallTranscriptRepository;
import com.lingring.domain.callanalysis.domain.transcript.CallTranscript;
import com.lingring.domain.callanalysis.dao.CallAnalysisRepository;
import com.lingring.domain.callanalysis.domain.analysis.CallAnalysis;
import com.lingring.domain.callanalysis.domain.analysis.CallAnalysisStatus;
import com.lingring.infrastructure.sqs.CallAnalysisResultMessage;
import com.lingring.infrastructure.sqs.CallAnalysisResultMessage.MistakeItemPart;
import com.lingring.infrastructure.sqs.CallAnalysisResultMessage.PositiveItemPart;
import com.lingring.infrastructure.sqs.CallAnalysisResultMessage.TranscriptPart;
import com.lingring.infrastructure.sqs.CallAnalysisResultMessage.UserAnalysisPart;
import com.lingring.domain.callanalysis.domain.transcript.vo.TranscriptSegment;
import com.lingring.global.config.ServiceIntegrationHelper;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class CallAnalysisCompletionFacadeTest extends ServiceIntegrationHelper {

    private static final Long CALL_ID = 100L;
    private static final Long USER_A = 1L;
    private static final Long USER_B = 2L;
    private static final String MODEL = "gemini-2.5-flash";

    @Autowired
    private CallAnalysisCompletionFacade callAnalysisCompletionFacade;

    @Autowired
    private CallTranscriptRepository callTranscriptRepository;

    @Autowired
    private CallAnalysisRepository callAnalysisRepository;

    @Test
    @DisplayName("transcript에 segments를 채우고 두 사용자 분석을 COMPLETED로 전이시킨다")
    void complete_marksAllAsCompleted() {
        // given
        callTranscriptRepository.save(CallTranscript.create(CALL_ID));
        callAnalysisRepository.save(CallAnalysis.processing(CALL_ID, USER_A));
        callAnalysisRepository.save(CallAnalysis.processing(CALL_ID, USER_B));

        final CallAnalysisResultMessage message = new CallAnalysisResultMessage(
                CALL_ID,
                MODEL,
                new TranscriptPart(List.of(
                        new TranscriptSegment(USER_A, 0.0, 2.1, "Hi"),
                        new TranscriptSegment(USER_B, 2.4, 5.0, "Hello")
                )),
                List.of(
                        new UserAnalysisPart(
                                USER_A,
                                List.of(new MistakeItemPart(
                                        "GRAMMAR",
                                        "I goed",
                                        "I went",
                                        "go의 과거형은 went",
                                        "갔다"
                                )),
                                List.of()
                        ),
                        new UserAnalysisPart(
                                USER_B,
                                List.of(),
                                List.of(new PositiveItemPart(
                                        "Sounds good.",
                                        "자연스러운 동의 표현",
                                        "좋아요."
                                ))
                        )
                )
        );

        // when
        callAnalysisCompletionFacade.complete(message);

        // then
        final CallTranscript transcript = callTranscriptRepository.findByCallId(CALL_ID).orElseThrow();
        assertThat(transcript.getContent().segments()).hasSize(2);

        final CallAnalysis a = callAnalysisRepository.findByCallIdAndUserId(CALL_ID, USER_A).orElseThrow();
        assertThat(a.getStatus()).isEqualTo(CallAnalysisStatus.COMPLETED);
        assertThat(a.getModelIdentifier()).isEqualTo(MODEL);
        assertThat(a.getResult().mistakes().count()).isEqualTo(1);

        final CallAnalysis b = callAnalysisRepository.findByCallIdAndUserId(CALL_ID, USER_B).orElseThrow();
        assertThat(b.getStatus()).isEqualTo(CallAnalysisStatus.COMPLETED);
        assertThat(b.getResult().positives().count()).isEqualTo(1);
    }
}
