package com.lingring.domain.matching.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willDoNothing;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import com.lingring.domain.matching.dto.response.MatchingStatusResponse;
import com.lingring.domain.matching.service.MatchingService;
import java.util.UUID;
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

@WebMvcTest(MatchingController.class)
class MatchingControllerTest {

    private static final UUID ROOM_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private MatchingService matchingService;

    @Nested
    @DisplayName("POST /api/v1/users/{userId}/matching")
    class EnterQueue {

        @Test
        @DisplayName("대기열 입장에 성공하면 204를 반환한다")
        void enterQueue_returns204() throws Exception {
            // given
            final Long userId = 1L;
            willDoNothing().given(matchingService).enterQueue(userId);

            // when
            final MockHttpServletResponse response = mockMvc.perform(
                            post("/api/v1/users/{userId}/matching", userId))
                    .andReturn()
                    .getResponse();

            // then
            assertThat(response.getStatus()).isEqualTo(204);
            then(matchingService).should().enterQueue(userId);
        }
    }

    @Nested
    @DisplayName("GET /api/v1/users/{userId}/matching")
    class GetStatus {

        @Test
        @DisplayName("매칭 결과가 있으면 MATCHED + partnerId + roomId를 반환한다")
        void getStatus_whenMatched_returnsMatched() throws Exception {
            // given
            final Long userId = 1L;
            given(matchingService.getStatus(userId))
                    .willReturn(MatchingStatusResponse.matched(2L, ROOM_ID));

            // when
            final MockHttpServletResponse response = mockMvc.perform(
                            get("/api/v1/users/{userId}/matching", userId)
                                    .accept(MediaType.APPLICATION_JSON))
                    .andReturn()
                    .getResponse();

            // then
            assertThat(response.getStatus()).isEqualTo(200);
            final JsonNode body = objectMapper.readTree(response.getContentAsString());
            assertThat(body.get("data").get("status").asText()).isEqualTo("MATCHED");
            assertThat(body.get("data").get("partnerId").asLong()).isEqualTo(2L);
            assertThat(body.get("data").get("roomId").asText()).isEqualTo(ROOM_ID.toString());
        }

        @Test
        @DisplayName("큐와 결과 모두 없으면 NONE을 반환한다")
        void getStatus_whenNone_returnsNone() throws Exception {
            // given
            final Long userId = 1L;
            given(matchingService.getStatus(userId))
                    .willReturn(MatchingStatusResponse.none());

            // when
            final MockHttpServletResponse response = mockMvc.perform(
                            get("/api/v1/users/{userId}/matching", userId)
                                    .accept(MediaType.APPLICATION_JSON))
                    .andReturn()
                    .getResponse();

            // then
            assertThat(response.getStatus()).isEqualTo(200);
            final JsonNode body = objectMapper.readTree(response.getContentAsString());
            assertThat(body.get("data").get("status").asText()).isEqualTo("NONE");
        }
    }

    @Nested
    @DisplayName("DELETE /api/v1/users/{userId}/matching")
    class LeaveQueue {

        @Test
        @DisplayName("취소 성공 시 204를 반환한다")
        void leaveQueue_returns204() throws Exception {
            // given
            final Long userId = 1L;
            willDoNothing().given(matchingService).leaveQueue(userId);

            // when
            final MockHttpServletResponse response = mockMvc.perform(
                            delete("/api/v1/users/{userId}/matching", userId))
                    .andReturn()
                    .getResponse();

            // then
            assertThat(response.getStatus()).isEqualTo(204);
            then(matchingService).should().leaveQueue(userId);
        }
    }
}
