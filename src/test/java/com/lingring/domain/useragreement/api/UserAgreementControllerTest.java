package com.lingring.domain.useragreement.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import com.lingring.domain.useragreement.domain.AgreementItem;
import com.lingring.domain.useragreement.dto.request.AgreementCreateRequest;
import com.lingring.domain.useragreement.dto.response.AgreementResponse;
import com.lingring.domain.useragreement.dto.response.AgreementResponse.UserSummary;
import com.lingring.domain.useragreement.service.UserAgreementService;
import com.lingring.global.auth.context.AuthContext;
import java.util.EnumSet;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@WebMvcTest(UserAgreementController.class)
class UserAgreementControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private UserAgreementService userAgreementService;

    @AfterEach
    void clearAuthContext() {
        AuthContext.clear();
    }

    @DisplayName("POST /api/v1/me/agreements")
    @org.junit.jupiter.api.Nested
    class AcceptAgreements {

        @Test
        @DisplayName("성공 시 200과 갱신된 user 정보(requiresOnboarding=false)를 반환")
        void accept_whenSuccess_returns200WithBody() throws Exception {
            // given
            final Long userId = 42L;
            AuthContext.set(userId);
            final AgreementCreateRequest request = new AgreementCreateRequest(
                    "2026-05-06",
                    EnumSet.allOf(AgreementItem.class)
            );
            given(userAgreementService.accept(eq(userId), any(AgreementCreateRequest.class)))
                    .willReturn(new AgreementResponse(
                            new UserSummary(userId, "링링이", null, false)
                    ));

            // when
            final MockHttpServletResponse response = mockMvc.perform(post("/api/v1/me/agreements")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andReturn()
                    .getResponse();

            // then
            assertThat(response.getStatus()).isEqualTo(200);
            final JsonNode body = objectMapper.readTree(response.getContentAsString());
            assertThat(body.get("data").get("user").get("id").asLong()).isEqualTo(userId);
            assertThat(body.get("data").get("user").get("requiresOnboarding").asBoolean()).isFalse();
        }

        @Test
        @DisplayName("termsVersion이 비어있으면 @Valid가 차단하고 service를 호출하지 않는다")
        void accept_whenTermsVersionBlank_rejectedByValidation() throws Exception {
            // given
            AuthContext.set(42L);
            final AgreementCreateRequest request = new AgreementCreateRequest(
                    "",
                    EnumSet.allOf(AgreementItem.class)
            );

            // when
            final MockHttpServletResponse response = mockMvc.perform(post("/api/v1/me/agreements")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andReturn()
                    .getResponse();

            // then
            final JsonNode body = objectMapper.readTree(response.getContentAsString());
            assertThat(body.get("status").asInt()).isEqualTo(400);
            then(userAgreementService).should(never()).accept(any(), any());
        }

        @Test
        @DisplayName("agreedItems가 비어있으면 @Valid가 차단하고 service를 호출하지 않는다")
        void accept_whenAgreedItemsEmpty_rejectedByValidation() throws Exception {
            // given
            AuthContext.set(42L);
            final AgreementCreateRequest request = new AgreementCreateRequest(
                    "2026-05-06",
                    EnumSet.noneOf(AgreementItem.class)
            );

            // when
            final MockHttpServletResponse response = mockMvc.perform(post("/api/v1/me/agreements")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andReturn()
                    .getResponse();

            // then
            final JsonNode body = objectMapper.readTree(response.getContentAsString());
            assertThat(body.get("status").asInt()).isEqualTo(400);
            then(userAgreementService).should(never()).accept(any(), any());
        }
    }
}
