package com.lingring.domain.presence.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.then;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import com.lingring.domain.presence.service.PresenceService;
import com.lingring.global.auth.context.AuthContext;
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
    private PresenceService presenceService;

    @AfterEach
    void clearAuthContext() {
        AuthContext.clear();
    }

    @Test
    @DisplayName("POST /api/v1/me/presence는 204를 반환하고 heartbeat를 위임한다")
    void heartbeat_returns204AndDelegates() throws Exception {
        // given
        final Long userId = 1L;
        AuthContext.set(userId);

        // when
        final MockHttpServletResponse response = mockMvc.perform(post("/api/v1/me/presence"))
                .andReturn()
                .getResponse();

        // then
        assertThat(response.getStatus()).isEqualTo(204);
        then(presenceService).should().heartbeat(userId);
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
