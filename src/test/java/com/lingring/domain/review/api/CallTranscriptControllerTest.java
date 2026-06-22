package com.lingring.domain.review.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import com.lingring.domain.review.dto.response.CallTranscriptResponse;
import com.lingring.domain.review.dto.response.TranscriptSegmentResponse;
import com.lingring.domain.review.exception.CallTranscriptNotReadyException;
import com.lingring.domain.review.service.CallTranscriptService;
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
    private CallTranscriptService callTranscriptService;

    @AfterEach
    void clearAuthContext() {
        AuthContext.clear();
    }

    @Nested
    @DisplayName("GET /api/v1/calls/{callId}/transcript")
    class GetTranscript {

        @Test
        @DisplayName("완료된 transcript면 200과 callId/segments를 반환한다")
        void getTranscript_whenCompleted_returns200() throws Exception {
            // given
            AuthContext.set(USER_ID);
            given(callTranscriptService.getTranscript(eq(CALL_ID), eq(USER_ID)))
                    .willReturn(new CallTranscriptResponse(CALL_ID, List.of(
                            new TranscriptSegmentResponse(1L, 0.0, 3.2, "first"),
                            new TranscriptSegmentResponse(2L, 3.5, 6.1, "second")
                    )));

            // when
            final MockHttpServletResponse response = mockMvc.perform(
                            get("/api/v1/calls/{callId}/transcript", CALL_ID))
                    .andReturn()
                    .getResponse();

            // then
            assertThat(response.getStatus()).isEqualTo(200);
            final JsonNode data = objectMapper.readTree(response.getContentAsString()).get("data");
            assertThat(data.get("callId").asLong()).isEqualTo(CALL_ID);
            assertThat(data.get("segments").size()).isEqualTo(2);
            assertThat(data.get("segments").get(0).get("text").asText()).isEqualTo("first");
            assertThat(data.get("segments").get(0).get("startSec").asDouble()).isEqualTo(0.0);
            assertThat(data.get("segments").get(0).get("userId").asLong()).isEqualTo(1L);
        }

        @Test
        @DisplayName("STT 미완료면 409와 status 409를 반환한다")
        void getTranscript_whenNotReady_returns409() throws Exception {
            // given
            AuthContext.set(USER_ID);
            given(callTranscriptService.getTranscript(eq(CALL_ID), eq(USER_ID)))
                    .willThrow(new CallTranscriptNotReadyException(CALL_ID));

            // when
            final MockHttpServletResponse response = mockMvc.perform(
                            get("/api/v1/calls/{callId}/transcript", CALL_ID))
                    .andReturn()
                    .getResponse();

            // then
            assertThat(response.getStatus()).isEqualTo(409);
            final JsonNode body = objectMapper.readTree(response.getContentAsString());
            assertThat(body.get("status").asInt()).isEqualTo(409);
        }
    }
}
