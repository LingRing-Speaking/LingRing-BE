package com.lingring.domain.push.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.then;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import com.lingring.domain.push.service.PushSendService;
import com.lingring.global.auth.context.AuthContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(PushTestController.class)
class PushTestControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PushSendService pushSendService;

    @AfterEach
    void clearAuthContext() {
        AuthContext.clear();
    }

    @Nested
    @DisplayName("POST /api/v1/me/push/test")
    class SendTest {

        @Test
        @DisplayName("성공 시 204를 반환하고 인증 유저로 테스트 발송을 위임한다")
        void sendTest_returns204() throws Exception {
            // given
            AuthContext.set(42L);

            // when
            final MockHttpServletResponse response = mockMvc.perform(post("/api/v1/me/push/test"))
                    .andReturn()
                    .getResponse();

            // then
            assertThat(response.getStatus()).isEqualTo(204);
            then(pushSendService).should().sendTest(42L);
        }
    }
}
