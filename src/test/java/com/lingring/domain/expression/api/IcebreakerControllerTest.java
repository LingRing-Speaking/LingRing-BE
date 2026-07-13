package com.lingring.domain.expression.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import com.lingring.domain.expression.dto.response.IcebreakerListResponse;
import com.lingring.domain.expression.dto.response.IcebreakerResponse;
import com.lingring.domain.expression.service.IcebreakerService;
import com.lingring.global.auth.context.AuthContext;
import com.lingring.global.error.ErrorCode;
import com.lingring.global.error.exception.NotFoundException;
import java.time.LocalDateTime;
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

@WebMvcTest(IcebreakerController.class)
class IcebreakerControllerTest {

    private static final Long USER_ID = 1L;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private IcebreakerService icebreakerService;

    @AfterEach
    void clearAuthContext() {
        AuthContext.clear();
    }

    @Nested
    @DisplayName("GET /api/v1/icebreakers")
    class GetRandom {

        @Test
        @DisplayName("count 파라미터로 요청 시 200 응답과 본문(bookmarkId 포함)을 반환한다")
        void getRandom_withCount_returns200WithItems() throws Exception {
            // given
            AuthContext.set(USER_ID);
            given(icebreakerService.getRandom(USER_ID, 3)).willReturn(
                    new IcebreakerListResponse(List.of(
                            new IcebreakerResponse(1L, "Hi!", "안녕!", LocalDateTime.now(), 900L),
                            new IcebreakerResponse(2L, "What's up?", "뭐해?", LocalDateTime.now(), null),
                            new IcebreakerResponse(3L, "Long time!", "오랜만!", LocalDateTime.now(), null)
                    ))
            );

            // when
            final MockHttpServletResponse response = mockMvc.perform(
                            get("/api/v1/icebreakers")
                                    .param("count", "3")
                                    .accept(MediaType.APPLICATION_JSON))
                    .andReturn()
                    .getResponse();

            // then
            assertThat(response.getStatus()).isEqualTo(200);
            final JsonNode body = objectMapper.readTree(response.getContentAsString());
            assertThat(body.get("status").asInt()).isEqualTo(200);
            assertThat(body.get("data").get("items")).hasSize(3);
            assertThat(body.get("data").get("items").get(0).get("expression").asText())
                    .isEqualTo("Hi!");
            assertThat(body.get("data").get("items").get(0).get("bookmarkId").asLong())
                    .isEqualTo(900L);
            assertThat(body.get("data").get("items").get(1).get("bookmarkId").isNull()).isTrue();
        }

        @Test
        @DisplayName("count 파라미터를 생략하면 디폴트 5로 서비스가 호출된다")
        void getRandom_withoutCount_usesDefault5() throws Exception {
            // given
            AuthContext.set(USER_ID);
            given(icebreakerService.getRandom(USER_ID, 5)).willReturn(
                    new IcebreakerListResponse(List.of(
                            new IcebreakerResponse(1L, "Hi!", "안녕!", LocalDateTime.now(), null)
                    ))
            );

            // when
            mockMvc.perform(get("/api/v1/icebreakers").accept(MediaType.APPLICATION_JSON))
                    .andReturn();

            // then
            then(icebreakerService).should().getRandom(USER_ID, 5);
        }

        @Test
        @DisplayName("아이스브레이커가 없으면 404 상태와 ICEBREAKER_NOT_FOUND 메시지를 반환한다")
        void getRandom_whenNotFound_returnsErrorMessage() throws Exception {
            // given
            AuthContext.set(USER_ID);
            willThrow(new NotFoundException(
                    ErrorCode.ICEBREAKER_NOT_FOUND,
                    "등록된 아이스브레이커가 없습니다."
            )).given(icebreakerService).getRandom(USER_ID, 5);

            // when
            final MockHttpServletResponse response = mockMvc.perform(
                            get("/api/v1/icebreakers")
                                    .accept(MediaType.APPLICATION_JSON))
                    .andReturn()
                    .getResponse();

            // then
            assertThat(response.getStatus())
                    .isEqualTo(ErrorCode.ICEBREAKER_NOT_FOUND.getHttpStatus().value());
            final JsonNode body = objectMapper.readTree(response.getContentAsString());
            assertThat(body.get("status").asInt())
                    .isEqualTo(ErrorCode.ICEBREAKER_NOT_FOUND.getHttpStatus().value());
            assertThat(body.get("message").asText())
                    .isEqualTo(ErrorCode.ICEBREAKER_NOT_FOUND.getMessage());
        }
    }
}
