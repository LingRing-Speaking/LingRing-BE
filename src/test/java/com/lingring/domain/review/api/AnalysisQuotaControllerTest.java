package com.lingring.domain.review.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import com.lingring.domain.review.dto.response.AnalysisQuotaResponse;
import com.lingring.domain.review.service.AnalysisQuotaService;
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
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@WebMvcTest(AnalysisQuotaController.class)
class AnalysisQuotaControllerTest {

    private static final Long USER_ID = 1L;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AnalysisQuotaService analysisQuotaService;

    @AfterEach
    void clearAuthContext() {
        AuthContext.clear();
    }

    @Test
    @DisplayName("GET /api/v1/me/analysis-quota → 200과 무료/유료 잔여, nextResetAt을 반환한다")
    void getMyQuota_returns200WithRemaining() throws Exception {
        // given
        AuthContext.set(USER_ID);
        given(analysisQuotaService.getStatus(eq(USER_ID)))
                .willReturn(new AnalysisQuotaResponse(1, 5, LocalDateTime.of(2026, 6, 29, 0, 0)));

        // when
        final MockHttpServletResponse response = mockMvc.perform(
                        get("/api/v1/me/analysis-quota"))
                .andReturn()
                .getResponse();

        // then
        assertThat(response.getStatus()).isEqualTo(200);
        final JsonNode data = objectMapper.readTree(response.getContentAsString()).get("data");
        assertThat(data.get("freeTicket").asInt()).isEqualTo(1);
        assertThat(data.get("paidTicket").asInt()).isEqualTo(5);
        assertThat(data.get("nextResetAt").asText()).startsWith("2026-06-29T00:00");
    }
}
