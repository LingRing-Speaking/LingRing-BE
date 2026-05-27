package com.lingring.infrastructure.sqs;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import com.lingring.domain.callanalysis.dao.CallTranscriptRepository;
import com.lingring.domain.callanalysis.domain.CallTranscript;
import com.lingring.domain.callanalysis.dao.CallAnalysisRepository;
import com.lingring.domain.callanalysis.domain.CallAnalysis;
import com.lingring.domain.callanalysis.domain.CallAnalysisStatus;
import com.lingring.domain.callanalysis.domain.vo.FeedbackTag;
import com.lingring.global.config.ServiceIntegrationHelper;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageResponse;

class CallAnalysisResultPollerTest extends ServiceIntegrationHelper {

    private static final Long CALL_ID = 138L;
    private static final Long USER_A = 1L;
    private static final Long USER_B = 14L;

    @Autowired
    private CallAnalysisResultPoller poller;

    @Autowired
    private CallTranscriptRepository callTranscriptRepository;

    @Autowired
    private CallAnalysisRepository callAnalysisRepository;

    @MockitoBean
    private SqsClient sqsClient;

    @Value("classpath:fixtures/call-analysis-result-sample.json")
    private Resource sampleMessage;

    @Test
    @DisplayName("SQS 메시지를 받아 JSON을 파싱하고 transcript + 두 사용자 분석을 모두 영속화한다")
    void poll_parsesSqsJsonAndPersistsTranscriptAndAnalyses() throws Exception {
        // given: POST 트리거가 이미 완료된 상태(PROCESSING 행 3개)
        callTranscriptRepository.save(CallTranscript.create(CALL_ID));
        callAnalysisRepository.save(CallAnalysis.processing(CALL_ID, USER_A));
        callAnalysisRepository.save(CallAnalysis.processing(CALL_ID, USER_B));

        final String body = sampleMessage.getContentAsString(StandardCharsets.UTF_8);
        final Message message = Message.builder()
                .messageId("test-message-1")
                .receiptHandle("test-receipt")
                .body(body)
                .build();
        given(sqsClient.receiveMessage(any(ReceiveMessageRequest.class)))
                .willReturn(ReceiveMessageResponse.builder()
                        .messages(message)
                        .build());

        // when
        poller.poll();

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

        // then: 성공 처리 후 ack(deleteMessage) 호출
        verify(sqsClient).deleteMessage(any(DeleteMessageRequest.class));
    }

    @Test
    @DisplayName("잘못된 JSON 메시지는 영속화하지 않고 ack도 호출하지 않는다")
    void poll_whenMalformedJson_doesNotPersistAndDoesNotAck() {
        // given
        callTranscriptRepository.save(CallTranscript.create(CALL_ID));
        callAnalysisRepository.save(CallAnalysis.processing(CALL_ID, USER_A));
        callAnalysisRepository.save(CallAnalysis.processing(CALL_ID, USER_B));

        final Message malformed = Message.builder()
                .messageId("bad-1")
                .receiptHandle("bad-receipt")
                .body("not-a-json")
                .build();
        given(sqsClient.receiveMessage(any(ReceiveMessageRequest.class)))
                .willReturn(ReceiveMessageResponse.builder().messages(malformed).build());

        // when
        poller.poll();

        // then: transcript content 미반영, analysis 상태 PROCESSING 유지
        assertThat(callTranscriptRepository.findByCallId(CALL_ID).orElseThrow().getContent()).isNull();
        assertThat(callAnalysisRepository.findByCallIdAndUserId(CALL_ID, USER_A).orElseThrow().getStatus())
                .isEqualTo(CallAnalysisStatus.PROCESSING);
        assertThat(callAnalysisRepository.findByCallIdAndUserId(CALL_ID, USER_B).orElseThrow().getStatus())
                .isEqualTo(CallAnalysisStatus.PROCESSING);
    }
}
