package com.lingring.domain.presence.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.lingring.domain.matching.dto.response.IncomingCallInvitationResponse;
import com.lingring.domain.presence.dto.response.HeartbeatResponse;
import com.lingring.domain.presence.facade.PresenceFacade;
import com.lingring.domain.presence.service.PresenceService;
import com.lingring.global.auth.context.AuthContext;
import java.time.LocalDateTime;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(PresenceController.class)
class PresenceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PresenceFacade presenceFacade;

    @MockitoBean
    private PresenceService presenceService;

    @AfterEach
    void clearAuthContext() {
        AuthContext.clear();
    }

    @Test
    @DisplayName("POST /api/v1/me/presence는 200을 반환하고 heartbeat를 위임한다 (수신 초대 없음 → null)")
    void heartbeat_whenNoIncomingInvitation_returns200WithNull() throws Exception {
        // given
        final Long userId = 1L;
        AuthContext.set(userId);
        given(presenceFacade.heartbeat(userId)).willReturn(HeartbeatResponse.empty());

        // when & then
        mockMvc.perform(post("/api/v1/me/presence"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.incomingInvitation").doesNotExist());
        then(presenceFacade).should().heartbeat(userId);
    }

    @Test
    @DisplayName("수신 중인 통화 초대가 있으면 heartbeat 응답에 실려 온다")
    void heartbeat_whenIncomingInvitation_returnsInvitation() throws Exception {
        // given
        final Long userId = 1L;
        AuthContext.set(userId);
        final LocalDateTime deadline = LocalDateTime.of(2026, 5, 12, 12, 0, 30);
        given(presenceFacade.heartbeat(userId))
                .willReturn(HeartbeatResponse.of(new IncomingCallInvitationResponse(2L, deadline)));

        // when & then
        mockMvc.perform(post("/api/v1/me/presence"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.incomingInvitation.inviterId").value(2L))
                .andExpect(jsonPath("$.data.incomingInvitation.deadline").exists());
    }

    @Test
    @DisplayName("DELETE /api/v1/me/presence는 204를 반환하고 disconnect를 위임한다")
    void disconnect_returns204AndDelegates() throws Exception {
        // given
        final Long userId = 1L;
        AuthContext.set(userId);

        // when
        final MockHttpServletResponse response = mockMvc.perform(delete("/api/v1/me/presence"))
                .andReturn()
                .getResponse();

        // then
        assertThat(response.getStatus()).isEqualTo(204);
        then(presenceService).should().disconnect(userId);
    }
}
