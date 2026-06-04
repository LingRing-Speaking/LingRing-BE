package com.lingring.domain.call.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import com.lingring.domain.call.dto.response.CallsResponse;
import com.lingring.domain.call.dto.response.CallSummaryResponse;
import com.lingring.domain.call.dto.response.PartnerResponse;
import com.lingring.domain.call.service.CallHistoryService;
import com.lingring.domain.call.dto.response.CallAnalysisStatusView;
import com.lingring.global.auth.context.AuthContext;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@WebMvcTest(CallController.class)
class CallControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private CallHistoryService callHistoryService;

    @AfterEach
    void clearAuthContext() {
        AuthContext.clear();
    }

    @Nested
    @DisplayName("GET /api/v1/calls")
    class GetAll {

        @Test
        @DisplayName("page/size를 명시하면 200 응답과 items + hasNext + analysisId + analysisStatus를 반환한다")
        void getAll_whenWithParams_returns200WithItemsAndHasNext() throws Exception {
            // given
            final Long userId = 1L;
            AuthContext.set(userId);
            final OffsetDateTime startedAt = LocalDateTime.of(2026, 4, 29, 19, 30)
                    .atZone(java.time.ZoneId.of("Asia/Seoul"))
                    .toOffsetDateTime();
            given(callHistoryService.getCallsByUserId(userId, 0, 2)).willReturn(
                    new CallsResponse(
                            List.of(
                                    new CallSummaryResponse(
                                            1042L,
                                            new PartnerResponse(7L, "Sophie", "https://cdn.example.com/p/sophie.png"),
                                            startedAt,
                                            312,
                                            777L,
                                            CallAnalysisStatusView.COMPLETED
                                    )
                            ),
                            true
                    )
            );

            // when
            final MockHttpServletResponse response = mockMvc.perform(
                            get("/api/v1/calls")
                                    .param("page", "0")
                                    .param("size", "2")
                                    .accept(MediaType.APPLICATION_JSON))
                    .andReturn()
                    .getResponse();

            // then
            assertThat(response.getStatus()).isEqualTo(200);
            final JsonNode body = objectMapper.readTree(response.getContentAsString());
            assertThat(body.get("status").asInt()).isEqualTo(200);
            final JsonNode data = body.get("data");
            assertThat(data.get("items").isArray()).isTrue();
            assertThat(data.get("items").size()).isEqualTo(1);
            final JsonNode first = data.get("items").get(0);
            assertThat(first.get("id").asLong()).isEqualTo(1042L);
            assertThat(first.get("partner").get("id").asLong()).isEqualTo(7L);
            assertThat(first.get("partner").get("name").asString()).isEqualTo("Sophie");
            assertThat(first.get("partner").get("profileImage").asString()).isEqualTo("https://cdn.example.com/p/sophie.png");
            assertThat(first.get("startedAt").asString()).isEqualTo("2026-04-29T19:30:00+09:00");
            assertThat(first.get("durationSec").asInt()).isEqualTo(312);
            assertThat(first.get("analysisId").asLong()).isEqualTo(777L);
            assertThat(first.get("analysisStatus").asString()).isEqualTo("COMPLETED");
            assertThat(data.get("hasNext").asBoolean()).isTrue();
        }

        @Test
        @DisplayName("분석 요청 안 한 통화는 analysisId=null, analysisStatus=READY 로 반환된다")
        void getAll_whenNotRequested_returnsReady() throws Exception {
            // given
            final Long userId = 1L;
            AuthContext.set(userId);
            final OffsetDateTime startedAt = LocalDateTime.of(2026, 4, 29, 19, 30)
                    .atZone(java.time.ZoneId.of("Asia/Seoul"))
                    .toOffsetDateTime();
            given(callHistoryService.getCallsByUserId(userId, 0, 20)).willReturn(
                    new CallsResponse(
                            List.of(
                                    new CallSummaryResponse(
                                            1042L,
                                            new PartnerResponse(7L, "Sophie", null),
                                            startedAt,
                                            120,
                                            null,
                                            CallAnalysisStatusView.READY
                                    )
                            ),
                            false
                    )
            );

            // when
            final MockHttpServletResponse response = mockMvc.perform(
                            get("/api/v1/calls").accept(MediaType.APPLICATION_JSON))
                    .andReturn()
                    .getResponse();

            // then
            assertThat(response.getStatus()).isEqualTo(200);
            final JsonNode data = objectMapper.readTree(response.getContentAsString()).get("data");
            assertThat(data.get("items").get(0).get("analysisId").isNull()).isTrue();
            assertThat(data.get("items").get(0).get("analysisStatus").asString()).isEqualTo("READY");
        }

        @Test
        @DisplayName("page/size를 생략하면 default(0, 20)로 service가 호출된다")
        void getAll_whenNoParams_usesDefaults() throws Exception {
            // given
            final Long userId = 1L;
            AuthContext.set(userId);
            given(callHistoryService.getCallsByUserId(userId, 0, 20)).willReturn(
                    new CallsResponse(List.of(), false)
            );

            // when
            mockMvc.perform(get("/api/v1/calls").accept(MediaType.APPLICATION_JSON));

            // then
            then(callHistoryService).should().getCallsByUserId(userId, 0, 20);
        }
    }
}
