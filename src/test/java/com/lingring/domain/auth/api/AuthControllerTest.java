package com.lingring.domain.auth.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import com.lingring.domain.auth.dto.request.DemoLoginRequest;
import com.lingring.domain.auth.dto.request.RefreshRequest;
import com.lingring.domain.auth.dto.request.SocialLoginRequest;
import com.lingring.domain.auth.dto.response.AuthTokenResponse;
import com.lingring.domain.auth.dto.response.AuthTokenResponse.UserSummary;
import com.lingring.domain.auth.dto.response.TokenPairResponse;
import com.lingring.domain.auth.facade.DemoLoginFacade;
import com.lingring.domain.auth.facade.SocialLoginFacade;
import com.lingring.domain.auth.service.AuthService;
import com.lingring.global.auth.context.AuthContext;
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

@WebMvcTest(AuthController.class)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private SocialLoginFacade socialLoginFacade;

    @MockitoBean
    private DemoLoginFacade demoLoginFacade;

    @AfterEach
    void clearAuthContext() {
        AuthContext.clear();
    }

    @Nested
    @DisplayName("POST /api/v1/auth/social")
    class SocialLogin {

        @Test
        @DisplayName("성공 시 200과 accessToken/refreshToken/user를 반환한다")
        void socialLogin_whenSuccess_returns200WithBody() throws Exception {
            // given
            final SocialLoginRequest request = new SocialLoginRequest(
                    "kakao", "id-token", null, "링링이", null
            );
            given(socialLoginFacade.socialLogin(any(SocialLoginRequest.class))).willReturn(
                    new AuthTokenResponse(
                            "access-jwt",
                            "refresh-jwt",
                            new UserSummary(42L, "링링이", null, false, "2026-06-30")
                    )
            );

            // when
            final MockHttpServletResponse response = mockMvc.perform(post("/api/v1/auth/social")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andReturn()
                    .getResponse();

            // then
            assertThat(response.getStatus()).isEqualTo(200);
            final JsonNode body = objectMapper.readTree(response.getContentAsString());
            assertThat(body.get("data").get("accessToken").asText()).isEqualTo("access-jwt");
            assertThat(body.get("data").get("refreshToken").asText()).isEqualTo("refresh-jwt");
            assertThat(body.get("data").get("user").get("id").asLong()).isEqualTo(42L);
            assertThat(body.get("data").get("user").get("nickname").asText()).isEqualTo("링링이");
        }

        @Test
        @DisplayName("idToken이 비어있으면 @Valid가 차단하고 service를 호출하지 않는다")
        void socialLogin_whenIdTokenBlank_rejectedByValidation() throws Exception {
            // given
            final SocialLoginRequest request = new SocialLoginRequest(
                    "kakao", "", null, "링링이", null
            );

            // when
            final MockHttpServletResponse response = mockMvc.perform(post("/api/v1/auth/social")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andReturn()
                    .getResponse();

            // then
            final JsonNode body = objectMapper.readTree(response.getContentAsString());
            assertThat(body.get("status").asInt()).isEqualTo(400);
            then(socialLoginFacade).should(never()).socialLogin(any());
        }

        @Test
        @DisplayName("idToken이 누락되면 @Valid가 차단하고 service를 호출하지 않는다")
        void socialLogin_whenIdTokenMissing_rejectedByValidation() throws Exception {
            // given — idToken=null
            final SocialLoginRequest request = new SocialLoginRequest(
                    "kakao", null, null, "링링이", null
            );

            // when
            final MockHttpServletResponse response = mockMvc.perform(post("/api/v1/auth/social")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andReturn()
                    .getResponse();

            // then
            final JsonNode body = objectMapper.readTree(response.getContentAsString());
            assertThat(body.get("status").asInt()).isEqualTo(400);
            then(socialLoginFacade).should(never()).socialLogin(any());
        }
    }

    @Nested
    @DisplayName("POST /api/v1/auth/refresh")
    class Refresh {

        @Test
        @DisplayName("body의 refresh token으로 갱신을 위임하고 200과 새 토큰 쌍을 반환한다")
        void refresh_whenValidBody_delegatesToService() throws Exception {
            // given
            final String refreshToken = "old-refresh-jwt";
            given(authService.refresh(eq(refreshToken))).willReturn(
                    new TokenPairResponse("new-access", "new-refresh")
            );

            // when
            final MockHttpServletResponse response = mockMvc.perform(post("/api/v1/auth/refresh")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new RefreshRequest(refreshToken))))
                    .andReturn()
                    .getResponse();

            // then
            assertThat(response.getStatus()).isEqualTo(200);
            final JsonNode body = objectMapper.readTree(response.getContentAsString());
            assertThat(body.get("data").get("accessToken").asText()).isEqualTo("new-access");
            assertThat(body.get("data").get("refreshToken").asText()).isEqualTo("new-refresh");
            assertThat(body.get("data").has("user")).isFalse();
            then(authService).should().refresh(refreshToken);
        }

        @Test
        @DisplayName("refreshToken이 비어있으면 @Valid가 차단하고 service를 호출하지 않는다")
        void refresh_whenRefreshTokenBlank_rejectedByValidation() throws Exception {
            // when
            final MockHttpServletResponse response = mockMvc.perform(post("/api/v1/auth/refresh")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new RefreshRequest(""))))
                    .andReturn()
                    .getResponse();

            // then
            final JsonNode body = objectMapper.readTree(response.getContentAsString());
            assertThat(body.get("status").asInt()).isEqualTo(400);
            then(authService).should(never()).refresh(any());
        }
    }

    @Nested
    @DisplayName("POST /api/v1/auth/demo-login")
    class DemoLogin {

        @Test
        @DisplayName("성공 시 200과 accessToken/refreshToken/user를 반환한다")
        void demoLogin_whenSuccess_returns200WithBody() throws Exception {
            // given
            final DemoLoginRequest request = new DemoLoginRequest("review-token-a");
            given(demoLoginFacade.demoLogin(eq("review-token-a"))).willReturn(
                    new AuthTokenResponse(
                            "demo-access",
                            "demo-refresh",
                            new UserSummary(7L, "Reviewer A", null, false, "2026-06-30")
                    )
            );

            // when
            final MockHttpServletResponse response = mockMvc.perform(post("/api/v1/auth/demo-login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andReturn()
                    .getResponse();

            // then
            assertThat(response.getStatus()).isEqualTo(200);
            final JsonNode body = objectMapper.readTree(response.getContentAsString());
            assertThat(body.get("data").get("accessToken").asText()).isEqualTo("demo-access");
            assertThat(body.get("data").get("user").get("nickname").asText()).isEqualTo("Reviewer A");
        }

        @Test
        @DisplayName("token이 비어있으면 @Valid가 차단하고 facade를 호출하지 않는다")
        void demoLogin_whenTokenBlank_rejectedByValidation() throws Exception {
            // given
            final DemoLoginRequest request = new DemoLoginRequest("");

            // when
            final MockHttpServletResponse response = mockMvc.perform(post("/api/v1/auth/demo-login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andReturn()
                    .getResponse();

            // then
            final JsonNode body = objectMapper.readTree(response.getContentAsString());
            assertThat(body.get("status").asInt()).isEqualTo(400);
            then(demoLoginFacade).should(never()).demoLogin(any());
        }

        @Test
        @DisplayName("token이 누락되면 @Valid가 차단하고 facade를 호출하지 않는다")
        void demoLogin_whenTokenMissing_rejectedByValidation() throws Exception {
            // given — token=null
            final DemoLoginRequest request = new DemoLoginRequest(null);

            // when
            final MockHttpServletResponse response = mockMvc.perform(post("/api/v1/auth/demo-login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andReturn()
                    .getResponse();

            // then
            final JsonNode body = objectMapper.readTree(response.getContentAsString());
            assertThat(body.get("status").asInt()).isEqualTo(400);
            then(demoLoginFacade).should(never()).demoLogin(any());
        }
    }

    @Nested
    @DisplayName("POST /api/v1/auth/logout")
    class Logout {

        @Test
        @DisplayName("AuthContext의 userId로 logout을 호출하고 204를 반환한다")
        void logout_whenAuthenticated_callsServiceWithUserId() throws Exception {
            // given
            AuthContext.set(42L);

            // when
            final MockHttpServletResponse response = mockMvc.perform(post("/api/v1/auth/logout"))
                    .andReturn()
                    .getResponse();

            // then
            then(authService).should().logout(42L);
            final JsonNode body = objectMapper.readTree(response.getContentAsString());
            assertThat(body.get("status").asInt()).isEqualTo(204);
        }
    }
}
