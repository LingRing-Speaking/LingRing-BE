package com.lingring.domain.user.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import com.lingring.domain.user.domain.Level;
import com.lingring.domain.user.dto.response.UserStatsResponse;
import com.lingring.domain.user.service.UserStatsService;
import com.lingring.global.error.ErrorCode;
import com.lingring.global.error.exception.NotFoundException;
import java.math.BigDecimal;
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

@WebMvcTest(UserStatsController.class)
class UserStatsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private UserStatsService userStatsService;

    @Nested
    @DisplayName("GET /api/v1/users/{userId}/stats")
    class GetStats {

        @Test
        @DisplayName("통계가 존재하면 200 응답과 통계 필드를 반환한다")
        void getStats_whenExists_returns200WithBody() throws Exception {
            // given
            final Long userId = 1L;
            given(userStatsService.getByUserId(userId)).willReturn(new UserStatsResponse(
                    userId, Level.BEGINNER, new BigDecimal("36.5"), 0, 0, 0, null
            ));

            // when
            final MockHttpServletResponse response = mockMvc.perform(get("/api/v1/users/{userId}/stats", userId)
                            .accept(MediaType.APPLICATION_JSON))
                    .andReturn()
                    .getResponse();

            // then
            assertThat(response.getStatus()).isEqualTo(200);
            final JsonNode body = objectMapper.readTree(response.getContentAsString());
            assertThat(body.get("status").asInt()).isEqualTo(200);
            assertThat(body.get("data").get("userId").asLong()).isEqualTo(userId);
            assertThat(body.get("data").get("level").asText()).isEqualTo("BEGINNER");
            assertThat(body.get("data").get("totalCallCount").asInt()).isZero();
            assertThat(body.get("data").get("currentStreakDays").asInt()).isZero();
            assertThat(body.get("data").get("expressionCount").asInt()).isZero();
            assertThat(body.get("data").get("lastStudyDate").isNull()).isTrue();
        }

        @Test
        @DisplayName("통계가 없으면 USER_STATS_NOT_FOUND 에러 메시지를 body에 담아 반환한다")
        void getStats_whenNotFound_returnsErrorMessage() throws Exception {
            // given
            final Long userId = 999L;
            willThrow(new NotFoundException(
                    ErrorCode.USER_STATS_NOT_FOUND,
                    "userId가 %d인 사용자 통계를 찾을 수 없습니다.".formatted(userId)
            )).given(userStatsService).getByUserId(userId);

            // when
            final MockHttpServletResponse response = mockMvc.perform(get("/api/v1/users/{userId}/stats", userId)
                            .accept(MediaType.APPLICATION_JSON))
                    .andReturn()
                    .getResponse();

            // then
            final JsonNode body = objectMapper.readTree(response.getContentAsString());
            assertThat(body.get("status").asInt()).isEqualTo(ErrorCode.USER_STATS_NOT_FOUND.getHttpStatus().value());
            assertThat(body.get("message").asText()).isEqualTo(ErrorCode.USER_STATS_NOT_FOUND.getMessage());
        }
    }
}
