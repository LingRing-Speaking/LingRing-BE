package com.lingring.infrastructure.sqs;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.lingring.domain.review.dao.CallAnalysisRepository;
import com.lingring.domain.review.dao.CallTranscriptRepository;
import com.lingring.domain.review.domain.analysis.CallAnalysis;
import com.lingring.domain.review.domain.analysis.CallAnalysisStatus;
import com.lingring.domain.review.domain.analysis.vo.FeedbackTag;
import com.lingring.domain.review.domain.transcript.CallTranscript;
import com.lingring.global.config.ServiceIntegrationHelper;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import tools.jackson.databind.ObjectMapper;

class CallAnalysisResultListenerTest extends ServiceIntegrationHelper {

    private static final Long CALL_ID = 138L;
    private static final Long USER_A = 1L;
    private static final Long USER_B = 14L;

    @Autowired
    private CallAnalysisResultListener listener;

    @Autowired
    private CallTranscriptRepository callTranscriptRepository;

    @Autowired
    private CallAnalysisRepository callAnalysisRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Value("classpath:fixtures/call-analysis-result-sample.json")
    private Resource sampleMessage;

    private CallAnalysisResultMessage sampleMessage() throws Exception {
        return objectMapper.readValue(
                sampleMessage.getContentAsString(StandardCharsets.UTF_8),
                CallAnalysisResultMessage.class);
    }

    @Test
    @DisplayName("역직렬화된 결과 메시지를 받아 transcript + 두 사용자 분석을 모두 영속화한다")
    void handle_persistsTranscriptAndBothUserAnalyses() throws Exception {
        // given: POST 트리거가 이미 완료된 상태(PROCESSING 행 3개)
        callTranscriptRepository.save(CallTranscript.create(CALL_ID));
        callAnalysisRepository.save(CallAnalysis.processing(CALL_ID, USER_A));
        callAnalysisRepository.save(CallAnalysis.processing(CALL_ID, USER_B));

        // when
        listener.handle(sampleMessage());

        // then: transcript 24 segments
        final CallTranscript transcript = callTranscriptRepository.findByCallId(CALL_ID).orElseThrow();
        assertThat(transcript.getContent().segments()).hasSize(24);
        assertThat(transcript.getContent().segments().get(0).text()).isEqualTo("Hi.");
        assertThat(transcript.getContent().segments().get(0).userId()).isEqualTo(USER_B);

        // then: userId=1 분석
        final CallAnalysis a1 = callAnalysisRepository.findByCallIdAndUserId(CALL_ID, USER_A).orElseThrow();
        assertThat(a1.getStatus()).isEqualTo(CallAnalysisStatus.COMPLETED);
        assertThat(a1.getModelIdentifier()).isEqualTo("gemini-2.5-flash");
        assertThat(a1.getResult().mistakes().count()).isEqualTo(5);
        assertThat(a1.getResult().positives().count()).isEqualTo(3);
        assertThat(a1.getResult().mistakes().items().get(0).tag()).isEqualTo(FeedbackTag.COLLOCATION);
        assertThat(a1.getResult().mistakes().items().get(0).improved()).isEqualTo("this weekend");
        assertThat(a1.getResult().positives().items().get(0).sentence()).isEqualTo("Oh, hi, my name is Lee.");

        // then: userId=14 분석
        final CallAnalysis a14 = callAnalysisRepository.findByCallIdAndUserId(CALL_ID, USER_B).orElseThrow();
        assertThat(a14.getStatus()).isEqualTo(CallAnalysisStatus.COMPLETED);
        assertThat(a14.getModelIdentifier()).isEqualTo("gemini-2.5-flash");
        assertThat(a14.getResult().mistakes().count()).isEqualTo(5);
        assertThat(a14.getResult().positives().count()).isEqualTo(3);
    }

    @Test
    @DisplayName("영속화 실패 시 예외를 전파한다 (프레임워크가 ack하지 않고 재전달하도록)")
    void handle_propagatesException_whenPersistenceFails() throws Exception {
        // given: PROCESSING 행이 없어 complete()가 실패하는 상태 (beforeEach가 DB를 비움)

        // when / then: 예외를 삼키지 않고 그대로 전파
        assertThatThrownBy(() -> listener.handle(sampleMessage()))
                .isInstanceOf(RuntimeException.class);

        // and: 트랜잭션 롤백으로 아무것도 영속화되지 않음
        assertThat(callTranscriptRepository.findByCallId(CALL_ID)).isEmpty();
    }
}
