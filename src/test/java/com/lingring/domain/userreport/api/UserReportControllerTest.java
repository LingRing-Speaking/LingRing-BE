package com.lingring.domain.userreport.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import com.lingring.domain.userreport.domain.ReportReason;
import com.lingring.domain.userreport.dto.request.UserReportCreateRequest;
import com.lingring.domain.userreport.dto.response.UserReportResponse;
import com.lingring.domain.userreport.service.UserReportService;
import com.lingring.global.error.ErrorCode;
import com.lingring.global.error.exception.BadRequestException;
import java.time.LocalDateTime;
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

@WebMvcTest(UserReportController.class)
class UserReportControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private UserReportService userReportService;

    @Nested
    @DisplayName("POST /users/{userId}/reports")
    class Report {

        @Test
        @DisplayName("유효한 요청이면 201 응답과 신고 정보(reasonLabel 포함)를 반환한다")
        void report_whenValid_returns201WithBody() throws Exception {
            // given
            final Long userId = 1L;
            final UserReportCreateRequest request = new UserReportCreateRequest(
                    2L, ReportReason.INAPPROPRIATE_CONVERSATION, "도배성 메시지를 반복적으로 보냄"
            );
            given(userReportService.report(eq(userId), any(UserReportCreateRequest.class)))
                    .willReturn(new UserReportResponse(
                            10L, userId, 2L,
                            ReportReason.INAPPROPRIATE_CONVERSATION,
                            "부적절한 대화",
                            "도배성 메시지를 반복적으로 보냄",
                            LocalDateTime.now()
                    ));

            // when
            final MockHttpServletResponse response = mockMvc.perform(
                            post("/users/{userId}/reports", userId)
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(request)))
                    .andReturn()
                    .getResponse();

            // then
            assertThat(response.getStatus()).isEqualTo(201);
            final JsonNode body = objectMapper.readTree(response.getContentAsString());
            assertThat(body.get("status").asInt()).isEqualTo(201);
            assertThat(body.get("data").get("id").asLong()).isEqualTo(10L);
            assertThat(body.get("data").get("reportedUserId").asLong()).isEqualTo(2L);
            assertThat(body.get("data").get("reason").asText()).isEqualTo("INAPPROPRIATE_CONVERSATION");
            assertThat(body.get("data").get("reasonLabel").asText()).isEqualTo("부적절한 대화");
            assertThat(body.get("data").get("description").asText())
                    .isEqualTo("도배성 메시지를 반복적으로 보냄");
        }

        @Test
        @DisplayName("자기 자신 신고면 400 상태와 SELF_REPORT_NOT_ALLOWED 메시지를 반환한다")
        void report_whenSelfReport_returnsErrorMessage() throws Exception {
            // given
            final Long userId = 1L;
            final UserReportCreateRequest request = new UserReportCreateRequest(
                    userId, ReportReason.BAD_MANNERS, "자기 자신을 신고하는 시도"
            );
            willThrow(new BadRequestException(
                    ErrorCode.SELF_REPORT_NOT_ALLOWED,
                    "자기 자신을 신고하려 했습니다."
            )).given(userReportService).report(eq(userId), any(UserReportCreateRequest.class));

            // when
            final MockHttpServletResponse response = mockMvc.perform(
                            post("/users/{userId}/reports", userId)
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(request)))
                    .andReturn()
                    .getResponse();

            // then
            final JsonNode body = objectMapper.readTree(response.getContentAsString());
            assertThat(body.get("status").asInt())
                    .isEqualTo(ErrorCode.SELF_REPORT_NOT_ALLOWED.getHttpStatus().value());
            assertThat(body.get("message").asText())
                    .isEqualTo(ErrorCode.SELF_REPORT_NOT_ALLOWED.getMessage());
        }
    }
}
