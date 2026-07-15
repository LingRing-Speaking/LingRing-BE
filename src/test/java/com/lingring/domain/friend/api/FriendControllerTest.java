package com.lingring.domain.friend.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.Mockito.never;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import com.lingring.domain.friend.domain.FriendRelation;
import com.lingring.domain.friend.domain.FriendRequestDirection;
import com.lingring.domain.friend.domain.FriendshipStatus;
import com.lingring.domain.friend.dto.request.FriendRequestCreateRequest;
import com.lingring.domain.friend.dto.request.FriendshipUpdateRequest;
import com.lingring.domain.friend.dto.response.FriendItemResponse;
import com.lingring.domain.friend.dto.response.FriendSearchResponse;
import com.lingring.domain.friend.dto.response.FriendsResponse;
import com.lingring.domain.friend.dto.response.FriendshipResponse;
import com.lingring.domain.friend.dto.response.ReceivedCountResponse;
import com.lingring.domain.friend.service.FriendService;
import com.lingring.global.auth.context.AuthContext;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
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

@WebMvcTest(FriendController.class)
class FriendControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private FriendService friendService;

    @AfterEach
    void clearAuthContext() {
        AuthContext.clear();
    }

    @Nested
    @DisplayName("POST /api/v1/friends")
    class SendRequest {

        @Test
        @DisplayName("유효한 요청이면 201과 결과 상태(PENDING)를 반환한다")
        void sendRequest_whenValid_returns201() throws Exception {
            // given
            final Long userId = 1L;
            AuthContext.set(userId);
            given(friendService.sendRequest(eq(userId), any(FriendRequestCreateRequest.class)))
                    .willReturn(new FriendshipResponse(2L, FriendshipStatus.PENDING));

            // when
            final MockHttpServletResponse response = mockMvc.perform(
                            post("/api/v1/friends")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(new FriendRequestCreateRequest(2L))))
                    .andReturn()
                    .getResponse();

            // then
            assertThat(response.getStatus()).isEqualTo(201);
            final JsonNode data = objectMapper.readTree(response.getContentAsString()).get("data");
            assertThat(data.get("userId").asLong()).isEqualTo(2L);
            assertThat(data.get("status").asText()).isEqualTo("PENDING");
        }

        @Test
        @DisplayName("targetUserId가 null이면 @Valid가 차단하고 service를 호출하지 않는다")
        void sendRequest_whenTargetNull_rejected() throws Exception {
            // given
            AuthContext.set(1L);

            // when
            final MockHttpServletResponse response = mockMvc.perform(
                            post("/api/v1/friends")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(new FriendRequestCreateRequest(null))))
                    .andReturn()
                    .getResponse();

            // then
            assertThat(response.getStatus()).isEqualTo(400);
            then(friendService).should(never()).sendRequest(any(), any());
        }
    }

    @Nested
    @DisplayName("PATCH /api/v1/friends/{requesterId}")
    class Accept {

        @Test
        @DisplayName("수락 요청이면 200과 결과 상태(ACCEPTED)를 반환한다")
        void accept_whenValid_returns200() throws Exception {
            // given
            final Long userId = 1L;
            AuthContext.set(userId);
            given(friendService.accept(eq(userId), eq(2L), any(FriendshipUpdateRequest.class)))
                    .willReturn(new FriendshipResponse(2L, FriendshipStatus.ACCEPTED));

            // when
            final MockHttpServletResponse response = mockMvc.perform(
                            patch("/api/v1/friends/{requesterId}", 2L)
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(
                                            new FriendshipUpdateRequest(FriendshipStatus.ACCEPTED))))
                    .andReturn()
                    .getResponse();

            // then
            assertThat(response.getStatus()).isEqualTo(200);
            final JsonNode data = objectMapper.readTree(response.getContentAsString()).get("data");
            assertThat(data.get("status").asText()).isEqualTo("ACCEPTED");
        }

        @Test
        @DisplayName("status가 null이면 @Valid가 차단하고 service를 호출하지 않는다")
        void accept_whenStatusNull_rejected() throws Exception {
            // given
            AuthContext.set(1L);

            // when
            final MockHttpServletResponse response = mockMvc.perform(
                            patch("/api/v1/friends/{requesterId}", 2L)
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(new FriendshipUpdateRequest(null))))
                    .andReturn()
                    .getResponse();

            // then
            assertThat(response.getStatus()).isEqualTo(400);
            then(friendService).should(never()).accept(any(), any(), any());
        }
    }

    @Nested
    @DisplayName("DELETE /api/v1/friends/{targetUserId}")
    class Remove {

        @Test
        @DisplayName("제거 성공 시 204를 반환한다")
        void remove_whenSuccess_returns204() throws Exception {
            // given
            final Long userId = 1L;
            AuthContext.set(userId);
            willDoNothing().given(friendService).remove(userId, 2L);

            // when
            final MockHttpServletResponse response = mockMvc.perform(
                            delete("/api/v1/friends/{targetUserId}", 2L))
                    .andReturn()
                    .getResponse();

            // then
            assertThat(response.getStatus()).isEqualTo(204);
            then(friendService).should().remove(userId, 2L);
        }
    }

    @Nested
    @DisplayName("GET /api/v1/friends")
    class GetFriends {

        @Test
        @DisplayName("파라미터를 생략하면 status=ACCEPTED, page=0, size=20 기본값으로 호출된다")
        void getFriends_whenNoParams_usesDefaults() throws Exception {
            // given
            final Long userId = 1L;
            AuthContext.set(userId);
            given(friendService.getFriends(userId, FriendshipStatus.ACCEPTED, null, 0, 20))
                    .willReturn(new FriendsResponse(List.of(), false));

            // when
            mockMvc.perform(get("/api/v1/friends").accept(MediaType.APPLICATION_JSON));

            // then
            then(friendService).should().getFriends(userId, FriendshipStatus.ACCEPTED, null, 0, 20);
        }

        @Test
        @DisplayName("status=PENDING을 명시하면 200과 items를 반환한다")
        void getFriends_whenPending_returns200WithItems() throws Exception {
            // given
            final Long userId = 1L;
            AuthContext.set(userId);
            given(friendService.getFriends(userId, FriendshipStatus.PENDING, null, 0, 20))
                    .willReturn(new FriendsResponse(
                            List.of(new FriendItemResponse(
                                    2L, "보낸사람", null, FriendshipStatus.PENDING,
                                    FriendRequestDirection.RECEIVED, LocalDateTime.now(), false)),
                            false));

            // when
            final MockHttpServletResponse response = mockMvc.perform(
                            get("/api/v1/friends")
                                    .param("status", "PENDING")
                                    .accept(MediaType.APPLICATION_JSON))
                    .andReturn()
                    .getResponse();

            // then
            assertThat(response.getStatus()).isEqualTo(200);
            final JsonNode data = objectMapper.readTree(response.getContentAsString()).get("data");
            assertThat(data.get("items").size()).isEqualTo(1);
            assertThat(data.get("items").get(0).get("direction").asText()).isEqualTo("RECEIVED");
        }

        @Test
        @DisplayName("direction=RECEIVED를 명시하면 그대로 서비스에 전달된다")
        void getFriends_whenDirectionReceived_passedThrough() throws Exception {
            // given
            final Long userId = 1L;
            AuthContext.set(userId);
            given(friendService.getFriends(userId, FriendshipStatus.PENDING, FriendRequestDirection.RECEIVED, 0, 20))
                    .willReturn(new FriendsResponse(List.of(), false));

            // when
            mockMvc.perform(get("/api/v1/friends")
                    .param("status", "PENDING")
                    .param("direction", "RECEIVED")
                    .accept(MediaType.APPLICATION_JSON));

            // then
            then(friendService).should()
                    .getFriends(userId, FriendshipStatus.PENDING, FriendRequestDirection.RECEIVED, 0, 20);
        }
    }

    @Nested
    @DisplayName("GET /api/v1/friends/search")
    class Search {

        @Test
        @DisplayName("일치하는 사용자가 있으면 200과 relation 포함 결과를 반환한다")
        void search_whenFound_returns200WithRelation() throws Exception {
            // given
            final Long userId = 1L;
            AuthContext.set(userId);
            given(friendService.search(userId, "지우"))
                    .willReturn(Optional.of(new FriendSearchResponse(
                            7L, "지우", "https://cdn.example.com/j.png", FriendRelation.NONE)));

            // when
            final MockHttpServletResponse response = mockMvc.perform(
                            get("/api/v1/friends/search")
                                    .param("nickname", "지우")
                                    .accept(MediaType.APPLICATION_JSON))
                    .andReturn()
                    .getResponse();

            // then
            assertThat(response.getStatus()).isEqualTo(200);
            final JsonNode data = objectMapper.readTree(response.getContentAsString()).get("data");
            assertThat(data.get("userId").asLong()).isEqualTo(7L);
            assertThat(data.get("relation").asText()).isEqualTo("NONE");
        }

        @Test
        @DisplayName("일치하는 사용자가 없으면 200과 data=null을 반환한다")
        void search_whenNotFound_returnsNullData() throws Exception {
            // given
            final Long userId = 1L;
            AuthContext.set(userId);
            given(friendService.search(userId, "없는닉네임")).willReturn(Optional.empty());

            // when
            final MockHttpServletResponse response = mockMvc.perform(
                            get("/api/v1/friends/search")
                                    .param("nickname", "없는닉네임")
                                    .accept(MediaType.APPLICATION_JSON))
                    .andReturn()
                    .getResponse();

            // then
            assertThat(response.getStatus()).isEqualTo(200);
            assertThat(objectMapper.readTree(response.getContentAsString()).get("data").isNull()).isTrue();
        }
    }

    @Nested
    @DisplayName("GET /api/v1/friends/received-count")
    class ReceivedCount {

        @Test
        @DisplayName("받은 요청 개수를 200과 함께 반환한다")
        void receivedCount_returns200WithCount() throws Exception {
            // given
            final Long userId = 1L;
            AuthContext.set(userId);
            given(friendService.receivedRequestCount(userId))
                    .willReturn(new ReceivedCountResponse(3L));

            // when
            final MockHttpServletResponse response = mockMvc.perform(
                            get("/api/v1/friends/received-count").accept(MediaType.APPLICATION_JSON))
                    .andReturn()
                    .getResponse();

            // then
            assertThat(response.getStatus()).isEqualTo(200);
            final JsonNode data = objectMapper.readTree(response.getContentAsString()).get("data");
            assertThat(data.get("count").asLong()).isEqualTo(3L);
        }
    }
}
