package com.lingring.domain.user.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;

import com.lingring.domain.user.dto.request.NotificationSettingUpdateRequest;
import com.lingring.domain.user.dto.response.NotificationSettingResponse;
import com.lingring.domain.user.dto.response.UserSummaryResponse;
import com.lingring.domain.user.service.NotificationSettingService;
import com.lingring.global.auth.context.AuthContext;
import java.time.LocalDateTime;
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

@WebMvcTest(NotificationSettingController.class)
class NotificationSettingControllerTest {

    private static final String URL = "/api/v1/me/notification-settings";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private NotificationSettingService notificationSettingService;

    @AfterEach
    void clearAuthContext() {
        AuthContext.clear();
    }

    @Nested
    @DisplayName("PATCH /api/v1/me/notification-settings")
    class Update {

        @Test
        @DisplayName("성공 시 200과 수신 동의 상태가 담긴 user를 반환한다")
        void update_whenSuccess_returns200WithUser() throws Exception {
            // given
            final Long userId = 42L;
            AuthContext.set(userId);
            final LocalDateTime updatedAt = LocalDateTime.of(2026, 10, 2, 21, 3, 11);
            given(notificationSettingService.update(eq(userId), any(NotificationSettingUpdateRequest.class)))
                    .willReturn(new NotificationSettingResponse(
                            new UserSummaryResponse(userId, "링링이", null, false, "2026-06-30", true, updatedAt)
                    ));

            // when
            final MockHttpServletResponse response = perform("{\"marketingPush\": true}");

            // then
            assertThat(response.getStatus()).isEqualTo(200);
            final JsonNode user = objectMapper.readTree(response.getContentAsString()).get("data").get("user");
            assertThat(user.get("id").asLong()).isEqualTo(userId);
            assertThat(user.get("marketingPushAgreed").asBoolean()).isTrue();
            assertThat(user.get("marketingPushUpdatedAt").asString()).isEqualTo("2026-10-02T21:03:11");
        }

        @Test
        @DisplayName("marketingPush가 없으면 @Valid가 차단하고 service를 호출하지 않는다")
        void update_whenMarketingPushMissing_rejectedByValidation() throws Exception {
            // given
            AuthContext.set(42L);

            // when
            final MockHttpServletResponse response = perform("{}");

            // then
            final JsonNode body = objectMapper.readTree(response.getContentAsString());
            assertThat(body.get("status").asInt()).isEqualTo(400);
            then(notificationSettingService).should(never()).update(any(), any());
        }

        private MockHttpServletResponse perform(final String content) throws Exception {
            return mockMvc.perform(patch(URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(content))
                    .andReturn()
                    .getResponse();
        }
    }
}
