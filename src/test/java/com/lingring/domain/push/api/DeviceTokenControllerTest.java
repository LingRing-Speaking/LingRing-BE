package com.lingring.domain.push.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import com.lingring.domain.push.dto.request.DeviceTokenRegisterRequest;
import com.lingring.domain.push.dto.request.DeviceTokenUnregisterRequest;
import com.lingring.domain.push.service.DeviceTokenService;
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

@WebMvcTest(DeviceTokenController.class)
class DeviceTokenControllerTest {

    private static final Long USER_ID = 42L;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private DeviceTokenService deviceTokenService;

    @AfterEach
    void clearAuthContext() {
        AuthContext.clear();
    }

    @Nested
    @DisplayName("POST /api/v1/me/device-tokens")
    class Register {

        private static final String URL = "/api/v1/me/device-tokens";

        @Test
        @DisplayName("성공 시 204를 반환하고 인증 유저로 등록을 위임한다")
        void register_whenValid_returns204() throws Exception {
            // given
            AuthContext.set(USER_ID);
            final DeviceTokenRegisterRequest request = new DeviceTokenRegisterRequest("fcm-token", "ios");

            // when
            final MockHttpServletResponse response = perform(URL, objectMapper.writeValueAsString(request));

            // then
            assertThat(response.getStatus()).isEqualTo(204);
            then(deviceTokenService).should().register(eq(USER_ID), eq(request));
        }

        @Test
        @DisplayName("token이 비어 있으면 @Valid가 차단하고 service를 호출하지 않는다")
        void register_whenTokenBlank_rejectedByValidation() throws Exception {
            // given
            AuthContext.set(USER_ID);

            // when
            final MockHttpServletResponse response = perform(
                    URL,
                    objectMapper.writeValueAsString(new DeviceTokenRegisterRequest("", "ios"))
            );

            // then
            assertThat(statusOf(response)).isEqualTo(400);
            then(deviceTokenService).should(never()).register(any(), any());
        }

        @Test
        @DisplayName("platform이 없으면 @Valid가 차단하고 service를 호출하지 않는다")
        void register_whenPlatformMissing_rejectedByValidation() throws Exception {
            // given
            AuthContext.set(USER_ID);

            // when
            final MockHttpServletResponse response = perform(URL, "{\"token\": \"fcm-token\"}");

            // then
            assertThat(statusOf(response)).isEqualTo(400);
            then(deviceTokenService).should(never()).register(any(), any());
        }
    }

    @Nested
    @DisplayName("POST /api/v1/me/device-tokens/unregister")
    class Unregister {

        private static final String URL = "/api/v1/me/device-tokens/unregister";

        @Test
        @DisplayName("성공 시 204를 반환하고 인증 유저로 해제를 위임한다")
        void unregister_whenValid_returns204() throws Exception {
            // given
            AuthContext.set(USER_ID);
            final DeviceTokenUnregisterRequest request = new DeviceTokenUnregisterRequest("fcm-token");

            // when
            final MockHttpServletResponse response = perform(URL, objectMapper.writeValueAsString(request));

            // then
            assertThat(response.getStatus()).isEqualTo(204);
            then(deviceTokenService).should().unregister(eq(USER_ID), eq(request));
        }

        @Test
        @DisplayName("token이 비어 있으면 @Valid가 차단하고 service를 호출하지 않는다")
        void unregister_whenTokenBlank_rejectedByValidation() throws Exception {
            // given
            AuthContext.set(USER_ID);

            // when
            final MockHttpServletResponse response = perform(URL, "{\"token\": \"\"}");

            // then
            assertThat(statusOf(response)).isEqualTo(400);
            then(deviceTokenService).should(never()).unregister(any(), any());
        }
    }

    private MockHttpServletResponse perform(final String url, final String content) throws Exception {
        return mockMvc.perform(post(url)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(content))
                .andReturn()
                .getResponse();
    }

    private int statusOf(final MockHttpServletResponse response) throws Exception {
        return objectMapper.readTree(response.getContentAsString()).get("status").asInt();
    }
}
