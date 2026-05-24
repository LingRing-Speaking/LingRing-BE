package com.lingring.domain.call.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import com.lingring.domain.call.domain.CallTranscriptStatus;
import com.lingring.domain.call.domain.vo.TranscriptSegment;
import com.lingring.domain.call.dto.response.CallTranscriptResponse;
import com.lingring.domain.call.dto.response.CallTranscriptStartResponse;
import com.lingring.domain.call.service.CallTranscriptService;
import com.lingring.domain.callanalysis.facade.CallAnalysisRequestFacade;
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

@WebMvcTest(CallTranscriptController.class)
class CallTranscriptControllerTest {

    private static final Long USER_ID = 1L;
    private static final Long CALL_ID = 42L;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private CallAnalysisRequestFacade callAnalysisRequestFacade;

    @MockitoBean
    private CallTranscriptService callTranscriptService;

    @AfterEach
    void clearAuthContext() {
        AuthContext.clear();
    }

    @Nested
    @DisplayName("POST /api/v1/calls/{callId}/analysis")
    class RequestAnalysis {

        @Test
        @DisplayName("분석 트리거 성공 시 202와 transcriptId/status를 반환한다")
        void requestAnalysis_returns202() throws Exception {
            // given
            AuthContext.set(USER_ID);
            given(callAnalysisRequestFacade.request(eq(CALL_ID), eq(USER_ID)))
                    .willReturn(new CallTranscriptStartResponse(7L, CallTranscriptStatus.PROCESSING));

            // when
            final MockHttpServletResponse response = mockMvc.perform(
                            post("/api/v1/calls/{callId}/analysis", CALL_ID))
                    .andReturn()
                    .getResponse();

            // then
            assertThat(response.getStatus()).isEqualTo(202);
            final JsonNode data = objectMapper.readTree(response.getContentAsString()).get("data");
            assertThat(data.get("transcriptId").asLong()).isEqualTo(7L);
            assertThat(data.get("status").asText()).isEqualTo("PROCESSING");
        }

        @Test
        @DisplayName("이미 COMPLETED 상태인 통화도 202와 기존 transcriptId/status를 반환한다 (멱등)")
        void requestAnalysis_whenAlreadyCompleted_returnsExisting() throws Exception {
            // given
            AuthContext.set(USER_ID);
            given(callAnalysisRequestFacade.request(eq(CALL_ID), eq(USER_ID)))
                    .willReturn(new CallTranscriptStartResponse(7L, CallTranscriptStatus.COMPLETED));

            // when
            final MockHttpServletResponse response = mockMvc.perform(
                            post("/api/v1/calls/{callId}/analysis", CALL_ID))
                    .andReturn()
                    .getResponse();

            // then
            assertThat(response.getStatus()).isEqualTo(202);
            final JsonNode data = objectMapper.readTree(response.getContentAsString()).get("data");
            assertThat(data.get("status").asText()).isEqualTo("COMPLETED");
        }
    }

    @Nested
    @DisplayName("GET /api/v1/calls/{callId}/transcript")
    class GetTranscript {

        @Test
        @DisplayName("PROCESSING 상태에서는 200과 status만 반환하고 segments는 null이다")
        void getTranscript_whenProcessing_returnsStatusOnly() throws Exception {
            // given
            AuthContext.set(USER_ID);
            given(callTranscriptService.getTranscript(eq(CALL_ID), eq(USER_ID)))
                    .willReturn(new CallTranscriptResponse(CallTranscriptStatus.PROCESSING, null));

            // when
            final MockHttpServletResponse response = mockMvc.perform(
                            get("/api/v1/calls/{callId}/transcript", CALL_ID))
                    .andReturn()
                    .getResponse();

            // then
            assertThat(response.getStatus()).isEqualTo(200);
            final JsonNode data = objectMapper.readTree(response.getContentAsString()).get("data");
            assertThat(data.get("status").asText()).isEqualTo("PROCESSING");
            assertThat(data.get("segments").isNull()).isTrue();
        }

        @Test
        @DisplayName("COMPLETED 상태에서는 200과 segments를 반환한다")
        void getTranscript_whenCompleted_returnsSegments() throws Exception {
            // given
            AuthContext.set(USER_ID);
            given(callTranscriptService.getTranscript(eq(CALL_ID), eq(USER_ID)))
                    .willReturn(new CallTranscriptResponse(
                            CallTranscriptStatus.COMPLETED,
                            List.of(
                                    new TranscriptSegment(101L, 0.0, 2.1, "안녕"),
                                    new TranscriptSegment(202L, 2.5, 4.8, "오 안녕")
                            )
                    ));

            // when
            final MockHttpServletResponse response = mockMvc.perform(
                            get("/api/v1/calls/{callId}/transcript", CALL_ID))
                    .andReturn()
                    .getResponse();

            // then
            assertThat(response.getStatus()).isEqualTo(200);
            final JsonNode data = objectMapper.readTree(response.getContentAsString()).get("data");
            assertThat(data.get("status").asText()).isEqualTo("COMPLETED");
            assertThat(data.get("segments").size()).isEqualTo(2);
            assertThat(data.get("segments").get(0).get("text").asText()).isEqualTo("안녕");
        }
    }
}
