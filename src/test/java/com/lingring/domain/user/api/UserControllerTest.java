package com.lingring.domain.user.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import com.lingring.domain.user.dto.response.UserMyResponse;
import com.lingring.domain.user.service.UserService;
import com.lingring.global.error.ErrorCode;
import com.lingring.global.error.exception.NotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(UserController.class)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private UserService userService;

    @Nested
    @DisplayName("GET /users/{userId}/my")
    class GetMy {

        @Test
        @DisplayName("사용자가 존재하면 200 응답과 id, name을 반환한다")
        void getMy_whenUserExists_returns200WithBody() throws Exception {
            // given
            final Long userId = 1L;
            given(userService.getMy(userId)).willReturn(new UserMyResponse(userId, "링링"));

            // when
            final MockHttpServletResponse response = mockMvc.perform(get("/users/{userId}/my", userId)
                            .accept(MediaType.APPLICATION_JSON))
                    .andReturn()
                    .getResponse();

            // then
            assertThat(response.getStatus()).isEqualTo(200);
            final JsonNode body = objectMapper.readTree(response.getContentAsString());
            assertThat(body.get("status").asInt()).isEqualTo(200);
            assertThat(body.get("data").get("id").asLong()).isEqualTo(userId);
            assertThat(body.get("data").get("name").asText()).isEqualTo("링링");
        }

        @Test
        @DisplayName("사용자가 없으면 USER_NOT_FOUND 에러 메시지를 body에 담아 반환한다")
        void getMy_whenUserNotFound_returnsErrorMessage() throws Exception {
            // given
            final Long userId = 999L;
            willThrow(new NotFoundException(
                    ErrorCode.USER_NOT_FOUND,
                    "ID가 %d인 사용자를 찾을 수 없습니다.".formatted(userId)
            )).given(userService).getMy(userId);

            // when
            final MockHttpServletResponse response = mockMvc.perform(get("/users/{userId}/my", userId)
                            .accept(MediaType.APPLICATION_JSON))
                    .andReturn()
                    .getResponse();

            // then
            final JsonNode body = objectMapper.readTree(response.getContentAsString());
            assertThat(body.get("status").asInt()).isEqualTo(ErrorCode.USER_NOT_FOUND.getHttpStatus().value());
            assertThat(body.get("message").asText()).isEqualTo(ErrorCode.USER_NOT_FOUND.getMessage());
        }
    }
}
