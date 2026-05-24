package com.lingring.domain.callanalysis.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import com.lingring.domain.callanalysis.domain.CallAnalysisStatus;
import com.lingring.domain.callanalysis.dto.response.CallAnalysisStatusResponse;
import com.lingring.domain.callanalysis.service.CallAnalysisService;
import com.lingring.global.auth.context.AuthContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@WebMvcTest(CallAnalysisStatusController.class)
class CallAnalysisStatusControllerTest {

    private static final Long USER_ID = 1L;
    private static final Long CALL_ID = 42L;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private CallAnalysisService callAnalysisService;

    @AfterEach
    void clearAuthContext() {
        AuthContext.clear();
    }

    @Test
    @DisplayName("본인 분석의 status만 200으로 반환한다")
    void getStatus_returnsStatusOnly() throws Exception {
        // given
        AuthContext.set(USER_ID);
        given(callAnalysisService.getStatus(eq(CALL_ID), eq(USER_ID)))
                .willReturn(new CallAnalysisStatusResponse(CallAnalysisStatus.PROCESSING));

        // when
        final MockHttpServletResponse response = mockMvc.perform(
                        get("/api/v1/calls/{callId}/analysis/status", CALL_ID))
                .andReturn()
                .getResponse();

        // then
        assertThat(response.getStatus()).isEqualTo(200);
        final JsonNode data = objectMapper.readTree(response.getContentAsString()).get("data");
        assertThat(data.get("status").asText()).isEqualTo("PROCESSING");
    }
}
