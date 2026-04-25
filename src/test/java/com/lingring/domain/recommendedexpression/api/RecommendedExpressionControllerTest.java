package com.lingring.domain.recommendedexpression.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import com.lingring.domain.recommendedexpression.dto.response.RecommendedExpressionResponse;
import com.lingring.domain.recommendedexpression.service.RecommendedExpressionService;
import com.lingring.global.error.ErrorCode;
import com.lingring.global.error.exception.NotFoundException;
import java.time.LocalDateTime;
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

@WebMvcTest(RecommendedExpressionController.class)
class RecommendedExpressionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private RecommendedExpressionService recommendedExpressionService;

    @Nested
    @DisplayName("GET /recommended-expressions/daily")
    class GetDaily {

        @Test
        @DisplayName("추천 표현이 존재하면 200 응답과 본문을 반환한다")
        void getDaily_whenExists_returns200WithBody() throws Exception {
            // given
            given(recommendedExpressionService.getDaily()).willReturn(
                    new RecommendedExpressionResponse(
                            7L, "How are you?", "어떻게 지내세요?", LocalDateTime.now()
                    )
            );

            // when
            final MockHttpServletResponse response = mockMvc.perform(
                            get("/recommended-expressions/daily")
                                    .accept(MediaType.APPLICATION_JSON))
                    .andReturn()
                    .getResponse();

            // then
            assertThat(response.getStatus()).isEqualTo(200);
            final JsonNode body = objectMapper.readTree(response.getContentAsString());
            assertThat(body.get("status").asInt()).isEqualTo(200);
            assertThat(body.get("data").get("id").asLong()).isEqualTo(7L);
            assertThat(body.get("data").get("expression").asText()).isEqualTo("How are you?");
            assertThat(body.get("data").get("meaning").asText()).isEqualTo("어떻게 지내세요?");
        }

        @Test
        @DisplayName("추천 표현이 없으면 404 상태와 RECOMMENDED_EXPRESSION_NOT_FOUND 메시지를 반환한다")
        void getDaily_whenNotFound_returnsErrorMessage() throws Exception {
            // given
            willThrow(new NotFoundException(
                    ErrorCode.RECOMMENDED_EXPRESSION_NOT_FOUND,
                    "추천 표현이 등록되어 있지 않습니다."
            )).given(recommendedExpressionService).getDaily();

            // when
            final MockHttpServletResponse response = mockMvc.perform(
                            get("/recommended-expressions/daily")
                                    .accept(MediaType.APPLICATION_JSON))
                    .andReturn()
                    .getResponse();

            // then
            final JsonNode body = objectMapper.readTree(response.getContentAsString());
            assertThat(body.get("status").asInt())
                    .isEqualTo(ErrorCode.RECOMMENDED_EXPRESSION_NOT_FOUND.getHttpStatus().value());
            assertThat(body.get("message").asText())
                    .isEqualTo(ErrorCode.RECOMMENDED_EXPRESSION_NOT_FOUND.getMessage());
        }
    }
}
