package com.lingring.domain.review.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import com.lingring.domain.call.dto.response.CallAnalysisStatusView;
import com.lingring.domain.review.domain.analysis.CallAnalysisStatus;
import com.lingring.domain.review.dto.response.CallAnalysisResponse;
import com.lingring.domain.review.dto.response.CallAnalysisStartResponse;
import com.lingring.domain.review.dto.response.CallAnalysisStatusResponse;
import com.lingring.domain.review.dto.response.MistakeItemResponse;
import com.lingring.domain.review.dto.response.PositiveItemResponse;
import com.lingring.domain.review.facade.CallAnalysisRequestFacade;
import com.lingring.domain.review.service.CallAnalysisService;
import com.lingring.global.auth.context.AuthContext;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@WebMvcTest(CallAnalysisController.class)
class CallAnalysisControllerTest {

    private static final Long USER_ID = 1L;
    private static final Long CALL_ID = 42L;
    private static final Long ANALYSIS_ID = 100L;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private CallAnalysisRequestFacade callAnalysisRequestFacade;

    @MockitoBean
    private CallAnalysisService callAnalysisService;

    @AfterEach
    void clearAuthContext() {
        AuthContext.clear();
    }

    @Nested
    @DisplayName("POST /api/v1/calls/{callId}/analysis")
    class RequestAnalysis {

        @Test
        @DisplayName("분석 트리거 성공 시 202와 본인 analysisId를 반환한다")
        void requestAnalysis_returns202WithAnalysisId() throws Exception {
            // given
            AuthContext.set(USER_ID);
            given(callAnalysisRequestFacade.request(eq(CALL_ID), eq(USER_ID)))
                    .willReturn(new CallAnalysisStartResponse(ANALYSIS_ID));

            // when
            final MockHttpServletResponse response = mockMvc.perform(
                            post("/api/v1/calls/{callId}/analysis", CALL_ID))
                    .andReturn()
                    .getResponse();

            // then
            assertThat(response.getStatus()).isEqualTo(202);
            final JsonNode data = objectMapper.readTree(response.getContentAsString()).get("data");
            assertThat(data.get("analysisId").asLong()).isEqualTo(ANALYSIS_ID);
        }
    }

    @Nested
    @DisplayName("GET /api/v1/analyses/{analysisId}")
    class GetAnalysis {

        @Test
        @DisplayName("COMPLETED 상태이면 200과 mistakes/positives 배열을 반환한다")
        void getAnalysis_whenCompleted_returnsItems() throws Exception {
            // given
            AuthContext.set(USER_ID);
            given(callAnalysisService.get(eq(ANALYSIS_ID), eq(USER_ID)))
                    .willReturn(new CallAnalysisResponse(
                            CALL_ID,
                            USER_ID,
                            CallAnalysisStatus.COMPLETED,
                            "gemini-2.5-flash",
                            List.of(new MistakeItemResponse(
                                    "GRAMMAR",
                                    "I goes to school.",
                                    "I go to school.",
                                    "주어가 1인칭일 때는 go를 씁니다.",
                                    "나는 학교에 갑니다."
                            )),
                            List.of(new PositiveItemResponse(
                                    "Sounds good.",
                                    "짧고 자연스러운 동의 표현",
                                    "좋아요."
                            ))
                    ));

            // when
            final MockHttpServletResponse response = mockMvc.perform(
                            get("/api/v1/analyses/{analysisId}", ANALYSIS_ID))
                    .andReturn()
                    .getResponse();

            // then
            assertThat(response.getStatus()).isEqualTo(200);
            final JsonNode data = objectMapper.readTree(response.getContentAsString()).get("data");
            assertThat(data.get("status").asText()).isEqualTo("COMPLETED");
            assertThat(data.get("modelIdentifier").asText()).isEqualTo("gemini-2.5-flash");
            assertThat(data.get("mistakes").size()).isEqualTo(1);
            assertThat(data.get("mistakes").get(0).get("tag").asText()).isEqualTo("GRAMMAR");
            assertThat(data.get("positives").size()).isEqualTo(1);
            assertThat(data.get("positives").get(0).get("sentence").asText()).isEqualTo("Sounds good.");
        }

        @Test
        @DisplayName("PROCESSING 상태이면 200과 빈 배열을 반환한다")
        void getAnalysis_whenProcessing_returnsEmptyArrays() throws Exception {
            // given
            AuthContext.set(USER_ID);
            given(callAnalysisService.get(eq(ANALYSIS_ID), eq(USER_ID)))
                    .willReturn(new CallAnalysisResponse(
                            CALL_ID,
                            USER_ID,
                            CallAnalysisStatus.PROCESSING,
                            null,
                            List.of(),
                            List.of()
                    ));

            // when
            final MockHttpServletResponse response = mockMvc.perform(
                            get("/api/v1/analyses/{analysisId}", ANALYSIS_ID))
                    .andReturn()
                    .getResponse();

            // then
            assertThat(response.getStatus()).isEqualTo(200);
            final JsonNode data = objectMapper.readTree(response.getContentAsString()).get("data");
            assertThat(data.get("status").asText()).isEqualTo("PROCESSING");
            assertThat(data.get("mistakes").size()).isEqualTo(0);
            assertThat(data.get("positives").size()).isEqualTo(0);
        }
    }

    @Nested
    @DisplayName("GET /api/v1/analyses/{analysisId}/status")
    class GetStatus {

        @Test
        @DisplayName("본인 분석의 status만 200으로 반환한다")
        void getStatus_returnsStatusOnly() throws Exception {
            // given
            AuthContext.set(USER_ID);
            given(callAnalysisService.getStatus(eq(ANALYSIS_ID), eq(USER_ID)))
                    .willReturn(new CallAnalysisStatusResponse(CallAnalysisStatusView.PROCESSING));

            // when
            final MockHttpServletResponse response = mockMvc.perform(
                            get("/api/v1/analyses/{analysisId}/status", ANALYSIS_ID))
                    .andReturn()
                    .getResponse();

            // then
            assertThat(response.getStatus()).isEqualTo(200);
            final JsonNode data = objectMapper.readTree(response.getContentAsString()).get("data");
            assertThat(data.get("status").asText()).isEqualTo("PROCESSING");
        }
    }
}
