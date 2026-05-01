package com.lingring.domain.userblock.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import com.lingring.domain.userblock.dto.request.UserBlockCreateRequest;
import com.lingring.domain.userblock.dto.response.UserBlockListResponse;
import com.lingring.domain.userblock.dto.response.UserBlockResponse;
import com.lingring.domain.userblock.service.UserBlockService;
import com.lingring.global.auth.context.AuthContext;
import com.lingring.global.error.ErrorCode;
import com.lingring.global.error.exception.BadRequestException;
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

@WebMvcTest(UserBlockController.class)
class UserBlockControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private UserBlockService userBlockService;

    @AfterEach
    void clearAuthContext() {
        AuthContext.clear();
    }

    @Nested
    @DisplayName("POST /api/v1/blocks")
    class Block {

        @Test
        @DisplayName("유효한 요청이면 201 응답과 차단 정보를 반환한다")
        void block_whenValid_returns201WithBody() throws Exception {
            // given
            final Long userId = 1L;
            AuthContext.set(userId);
            final UserBlockCreateRequest request = new UserBlockCreateRequest(2L);
            given(userBlockService.block(eq(userId), any(UserBlockCreateRequest.class)))
                    .willReturn(new UserBlockResponse(10L, userId, 2L, LocalDateTime.now()));

            // when
            final MockHttpServletResponse response = mockMvc.perform(
                            post("/api/v1/blocks")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(request)))
                    .andReturn()
                    .getResponse();

            // then
            assertThat(response.getStatus()).isEqualTo(201);
            final JsonNode body = objectMapper.readTree(response.getContentAsString());
            assertThat(body.get("status").asInt()).isEqualTo(201);
            assertThat(body.get("data").get("id").asLong()).isEqualTo(10L);
            assertThat(body.get("data").get("blockedUserId").asLong()).isEqualTo(2L);
        }

        @Test
        @DisplayName("자기 자신 차단이면 400 상태와 SELF_BLOCK_NOT_ALLOWED 메시지를 반환한다")
        void block_whenSelfBlock_returnsErrorMessage() throws Exception {
            // given
            final Long userId = 1L;
            AuthContext.set(userId);
            final UserBlockCreateRequest request = new UserBlockCreateRequest(userId);
            willThrow(new BadRequestException(
                    ErrorCode.SELF_BLOCK_NOT_ALLOWED,
                    "자기 자신을 차단하려 했습니다."
            )).given(userBlockService).block(eq(userId), any(UserBlockCreateRequest.class));

            // when
            final MockHttpServletResponse response = mockMvc.perform(
                            post("/api/v1/blocks")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(request)))
                    .andReturn()
                    .getResponse();

            // then
            final JsonNode body = objectMapper.readTree(response.getContentAsString());
            assertThat(body.get("status").asInt())
                    .isEqualTo(ErrorCode.SELF_BLOCK_NOT_ALLOWED.getHttpStatus().value());
            assertThat(body.get("message").asText())
                    .isEqualTo(ErrorCode.SELF_BLOCK_NOT_ALLOWED.getMessage());
        }

        @Test
        @DisplayName("blockedUserId가 null이면 @Valid가 차단하고 service를 호출하지 않는다")
        void block_whenBlockedUserIdNull_rejectedByValidation() throws Exception {
            // given
            final Long userId = 1L;
            AuthContext.set(userId);
            final UserBlockCreateRequest request = new UserBlockCreateRequest(null);

            // when
            final MockHttpServletResponse response = mockMvc.perform(
                            post("/api/v1/blocks")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(request)))
                    .andReturn()
                    .getResponse();

            // then
            final JsonNode body = objectMapper.readTree(response.getContentAsString());
            assertThat(body.get("status").asInt()).isEqualTo(400);
            then(userBlockService).should(never()).block(any(), any());
        }

        @Test
        @DisplayName("blockedUserId가 음수면 @Valid가 차단하고 service를 호출하지 않는다")
        void block_whenBlockedUserIdNegative_rejectedByValidation() throws Exception {
            // given
            final Long userId = 1L;
            AuthContext.set(userId);
            final UserBlockCreateRequest request = new UserBlockCreateRequest(-1L);

            // when
            final MockHttpServletResponse response = mockMvc.perform(
                            post("/api/v1/blocks")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(request)))
                    .andReturn()
                    .getResponse();

            // then
            final JsonNode body = objectMapper.readTree(response.getContentAsString());
            assertThat(body.get("status").asInt()).isEqualTo(400);
            then(userBlockService).should(never()).block(any(), any());
        }
    }

    @Nested
    @DisplayName("DELETE /api/v1/blocks/{blockedUserId}")
    class Unblock {

        @Test
        @DisplayName("차단 해제 성공 시 204를 반환한다")
        void unblock_whenSuccess_returns204() throws Exception {
            // given
            final Long userId = 1L;
            AuthContext.set(userId);
            final Long blockedUserId = 2L;
            willDoNothing().given(userBlockService).unblock(userId, blockedUserId);

            // when
            final MockHttpServletResponse response = mockMvc.perform(
                            delete("/api/v1/blocks/{blockedUserId}", blockedUserId))
                    .andReturn()
                    .getResponse();

            // then
            assertThat(response.getStatus()).isEqualTo(204);
            then(userBlockService).should().unblock(userId, blockedUserId);
        }
    }

    @Nested
    @DisplayName("GET /api/v1/blocks")
    class GetAll {

        @Test
        @DisplayName("page/size를 명시하면 200 응답과 items + hasNext를 반환한다")
        void getAll_whenWithParams_returns200WithItemsAndHasNext() throws Exception {
            // given
            final Long userId = 1L;
            AuthContext.set(userId);
            given(userBlockService.getAllByUserId(userId, 0, 2)).willReturn(
                    new UserBlockListResponse(
                            List.of(
                                    new UserBlockResponse(2L, userId, 11L, LocalDateTime.now()),
                                    new UserBlockResponse(1L, userId, 10L, LocalDateTime.now())
                            ),
                            true
                    )
            );

            // when
            final MockHttpServletResponse response = mockMvc.perform(
                            get("/api/v1/blocks")
                                    .param("page", "0")
                                    .param("size", "2")
                                    .accept(MediaType.APPLICATION_JSON))
                    .andReturn()
                    .getResponse();

            // then
            assertThat(response.getStatus()).isEqualTo(200);
            final JsonNode body = objectMapper.readTree(response.getContentAsString());
            final JsonNode data = body.get("data");
            assertThat(data.get("items").size()).isEqualTo(2);
            assertThat(data.get("hasNext").asBoolean()).isTrue();
        }

        @Test
        @DisplayName("page/size를 생략하면 default(0, 20)로 서비스가 호출된다")
        void getAll_whenNoParams_usesDefaults() throws Exception {
            // given
            final Long userId = 1L;
            AuthContext.set(userId);
            given(userBlockService.getAllByUserId(userId, 0, 20))
                    .willReturn(new UserBlockListResponse(List.of(), false));

            // when
            mockMvc.perform(get("/api/v1/blocks")
                    .accept(MediaType.APPLICATION_JSON));

            // then
            then(userBlockService).should().getAllByUserId(userId, 0, 20);
        }
    }
}
