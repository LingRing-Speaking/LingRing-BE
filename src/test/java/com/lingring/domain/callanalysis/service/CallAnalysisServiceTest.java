package com.lingring.domain.callanalysis.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.lingring.domain.callanalysis.dao.CallAnalysisRepository;
import com.lingring.domain.callanalysis.domain.CallAnalysis;
import com.lingring.domain.callanalysis.domain.CallAnalysisStatus;
import com.lingring.domain.callanalysis.domain.vo.AnalysisResult;
import com.lingring.domain.callanalysis.domain.vo.FeedbackTag;
import com.lingring.domain.callanalysis.domain.vo.MistakeItem;
import com.lingring.domain.callanalysis.domain.vo.Mistakes;
import com.lingring.domain.callanalysis.domain.vo.PositiveItem;
import com.lingring.domain.callanalysis.domain.vo.Positives;
import com.lingring.domain.callanalysis.dto.response.CallAnalysisResponse;
import com.lingring.domain.callanalysis.dto.response.CallAnalysisStatusResponse;
import com.lingring.domain.callanalysis.exception.CallAnalysisNotFoundException;
import com.lingring.global.config.ServiceIntegrationHelper;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class CallAnalysisServiceTest extends ServiceIntegrationHelper {

    private static final Long CALL_ID = 100L;
    private static final Long USER_ID = 1L;
    private static final String MODEL = "gemini-2.5-flash";

    @Autowired
    private CallAnalysisService callAnalysisService;

    @Autowired
    private CallAnalysisRepository callAnalysisRepository;

    @Nested
    @DisplayName("startProcessing: PROCESSING 행 생성")
    class StartProcessing {

        @Test
        @DisplayName("기존 행이 없으면 PROCESSING 상태로 새 행을 저장한다")
        void startProcessing_whenNotExists_createsRow() {
            // when
            callAnalysisService.startProcessing(CALL_ID, USER_ID);

            // then
            final CallAnalysis saved = callAnalysisRepository.findByCallIdAndUserId(CALL_ID, USER_ID).orElseThrow();
            assertThat(saved.getStatus()).isEqualTo(CallAnalysisStatus.PROCESSING);
            assertThat(saved.getResult()).isNull();
        }

        @Test
        @DisplayName("이미 행이 존재하면 멱등하게 중복 저장하지 않는다")
        void startProcessing_whenAlreadyExists_isIdempotent() {
            // given
            callAnalysisService.startProcessing(CALL_ID, USER_ID);
            final Long initialId = callAnalysisRepository.findByCallIdAndUserId(CALL_ID, USER_ID).orElseThrow().getId();

            // when
            callAnalysisService.startProcessing(CALL_ID, USER_ID);

            // then
            assertThat(callAnalysisRepository.findByCallIdAndUserId(CALL_ID, USER_ID).orElseThrow().getId())
                    .isEqualTo(initialId);
        }
    }

    @Nested
    @DisplayName("complete: PROCESSING → COMPLETED 전이")
    class Complete {

        @Test
        @DisplayName("PROCESSING 상태에서 COMPLETED로 전이하고 result/modelIdentifier가 채워진다")
        void complete_whenProcessing_transitions() {
            // given
            callAnalysisService.startProcessing(CALL_ID, USER_ID);
            final AnalysisResult result = sampleResult();

            // when
            callAnalysisService.complete(CALL_ID, USER_ID, result, MODEL);

            // then
            final CallAnalysis saved = callAnalysisRepository.findByCallIdAndUserId(CALL_ID, USER_ID).orElseThrow();
            assertThat(saved.getStatus()).isEqualTo(CallAnalysisStatus.COMPLETED);
            assertThat(saved.getModelIdentifier()).isEqualTo(MODEL);
            assertThat(saved.getResult().mistakes().count()).isEqualTo(1);
            assertThat(saved.getResult().positives().count()).isEqualTo(1);
        }

        @Test
        @DisplayName("이미 COMPLETED 상태에서 다시 호출하면 무시된다 (멱등)")
        void complete_whenAlreadyCompleted_isIdempotent() {
            // given
            callAnalysisService.startProcessing(CALL_ID, USER_ID);
            callAnalysisService.complete(CALL_ID, USER_ID, sampleResult(), MODEL);

            // when: 다른 model로 다시 complete
            callAnalysisService.complete(CALL_ID, USER_ID, AnalysisResult.empty(), "different-model");

            // then: 기존 model/result 유지
            final CallAnalysis saved = callAnalysisRepository.findByCallIdAndUserId(CALL_ID, USER_ID).orElseThrow();
            assertThat(saved.getModelIdentifier()).isEqualTo(MODEL);
            assertThat(saved.getResult().mistakes().count()).isEqualTo(1);
        }

        @Test
        @DisplayName("행이 없으면 CallAnalysisNotFoundException")
        void complete_whenMissing_throws() {
            // when & then
            assertThatThrownBy(() ->
                    callAnalysisService.complete(CALL_ID, USER_ID, sampleResult(), MODEL))
                    .isInstanceOf(CallAnalysisNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("fail: PROCESSING → FAILED 전이")
    class Fail {

        @Test
        @DisplayName("PROCESSING 상태에서 FAILED로 전이한다")
        void fail_whenProcessing_transitions() {
            // given
            callAnalysisService.startProcessing(CALL_ID, USER_ID);

            // when
            callAnalysisService.fail(CALL_ID, USER_ID);

            // then
            assertThat(callAnalysisRepository.findByCallIdAndUserId(CALL_ID, USER_ID).orElseThrow().getStatus())
                    .isEqualTo(CallAnalysisStatus.FAILED);
        }

        @Test
        @DisplayName("행이 없으면 예외 없이 무시된다")
        void fail_whenMissing_doesNothing() {
            // when & then (no throw)
            callAnalysisService.fail(CALL_ID, USER_ID);
        }
    }

    @Nested
    @DisplayName("getStatus: 본인 분석 상태만 조회")
    class GetStatus {

        @Test
        @DisplayName("PROCESSING 상태이면 PROCESSING을 담은 응답을 반환한다")
        void getStatus_whenProcessing_returnsStatus() {
            // given
            callAnalysisService.startProcessing(CALL_ID, USER_ID);

            // when
            final CallAnalysisStatusResponse response = callAnalysisService.getStatus(CALL_ID, USER_ID);

            // then
            assertThat(response.status()).isEqualTo(CallAnalysisStatus.PROCESSING);
        }

        @Test
        @DisplayName("COMPLETED 상태이면 COMPLETED를 담은 응답을 반환한다")
        void getStatus_whenCompleted_returnsStatus() {
            // given
            callAnalysisService.startProcessing(CALL_ID, USER_ID);
            callAnalysisService.complete(CALL_ID, USER_ID, sampleResult(), MODEL);

            // when
            final CallAnalysisStatusResponse response = callAnalysisService.getStatus(CALL_ID, USER_ID);

            // then
            assertThat(response.status()).isEqualTo(CallAnalysisStatus.COMPLETED);
        }

        @Test
        @DisplayName("행이 없으면 CallAnalysisNotFoundException")
        void getStatus_whenMissing_throws() {
            // when & then
            assertThatThrownBy(() -> callAnalysisService.getStatus(CALL_ID, USER_ID))
                    .isInstanceOf(CallAnalysisNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("get: 분석 결과 조회")
    class Get {

        @Test
        @DisplayName("존재하면 응답 DTO로 변환해 반환한다")
        void get_whenExists_returnsResponse() {
            // given
            callAnalysisService.startProcessing(CALL_ID, USER_ID);
            callAnalysisService.complete(CALL_ID, USER_ID, sampleResult(), MODEL);

            // when
            final CallAnalysisResponse response = callAnalysisService.get(CALL_ID, USER_ID);

            // then
            assertThat(response.callId()).isEqualTo(CALL_ID);
            assertThat(response.userId()).isEqualTo(USER_ID);
            assertThat(response.status()).isEqualTo(CallAnalysisStatus.COMPLETED);
            assertThat(response.mistakes()).hasSize(1);
            assertThat(response.positives()).hasSize(1);
        }

        @Test
        @DisplayName("존재하지 않으면 CallAnalysisNotFoundException")
        void get_whenMissing_throws() {
            // when & then
            assertThatThrownBy(() -> callAnalysisService.get(CALL_ID, USER_ID))
                    .isInstanceOf(CallAnalysisNotFoundException.class);
        }
    }

    private AnalysisResult sampleResult() {
        return new AnalysisResult(
                new Mistakes(List.of(new MistakeItem(
                        FeedbackTag.GRAMMAR,
                        "I goes",
                        "I go",
                        "1인칭 주어",
                        "나는 간다"
                ))),
                new Positives(List.of(new PositiveItem(
                        "Sounds good.",
                        "자연스러운 동의 표현",
                        "좋아요."
                )))
        );
    }
}
