package com.lingring.domain.referral.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import com.lingring.domain.referral.dto.request.RedeemReferralRequest;
import com.lingring.domain.referral.dto.response.RedeemReferralResponse;
import com.lingring.domain.referral.dto.response.ReferralStatusResponse;
import com.lingring.domain.referral.exception.ReferralAlreadyRedeemedException;
import com.lingring.domain.referral.exception.ReferralPeriodExpiredException;
import com.lingring.domain.referral.exception.ReferralRejoinedUserException;
import com.lingring.domain.referral.exception.ReferralSelfNotAllowedException;
import com.lingring.domain.referral.facade.ReferralFacade;
import com.lingring.global.auth.context.AuthContext;
import com.lingring.global.error.ErrorCode;
import com.lingring.global.error.exception.NotFoundException;
import java.time.LocalDateTime;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@WebMvcTest(ReferralController.class)
class ReferralControllerTest {

    private static final Long USER_ID = 1L;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ReferralFacade referralFacade;

    @AfterEach
    void clearAuthContext() {
        AuthContext.clear();
    }

    @Nested
    @DisplayName("GET /api/v1/me/referral")
    class GetStatus {

        @Test
        @DisplayName("200과 redeemable, redeemableUntil을 반환한다")
        void getStatus_returns200WithBody() throws Exception {
            // given
            AuthContext.set(USER_ID);
            given(referralFacade.getStatus(USER_ID)).willReturn(
                    new ReferralStatusResponse(true, LocalDateTime.of(2026, 10, 10, 14, 0))
            );

            // when
            final MockHttpServletResponse response = mockMvc.perform(get("/api/v1/me/referral"))
                    .andReturn()
                    .getResponse();

            // then
            assertThat(response.getStatus()).isEqualTo(200);
            final JsonNode body = objectMapper.readTree(response.getContentAsString());
            assertThat(body.get("data").get("redeemable").asBoolean()).isTrue();
            assertThat(body.get("data").get("redeemableUntil").asText()).isEqualTo("2026-10-10T14:00:00");
        }
    }

    @Nested
    @DisplayName("POST /api/v1/me/referral/redeem")
    class Redeem {

        @Test
        @DisplayName("성공 시 200과 지급 후 입력자의 paidTicket을 반환한다")
        void redeem_whenSuccess_returns200WithPaidTicket() throws Exception {
            // given
            AuthContext.set(USER_ID);
            given(referralFacade.redeem(USER_ID, "추천인")).willReturn(new RedeemReferralResponse(3));

            // when
            final MockHttpServletResponse response = perform(new RedeemReferralRequest("추천인"));

            // then
            assertThat(response.getStatus()).isEqualTo(200);
            final JsonNode body = objectMapper.readTree(response.getContentAsString());
            assertThat(body.get("data").get("paidTicket").asInt()).isEqualTo(3);
        }

        @Test
        @DisplayName("nickname이 비어 있으면 @Valid가 차단하고 facade를 호출하지 않는다")
        void redeem_whenNicknameBlank_rejectedByValidation() throws Exception {
            // given
            AuthContext.set(USER_ID);

            // when
            final MockHttpServletResponse response = perform(new RedeemReferralRequest(" "));

            // then
            assertThat(response.getStatus()).isEqualTo(400);
            then(referralFacade).should(never()).redeem(anyLong(), any());
        }

        @ParameterizedTest(name = "{1}")
        @MethodSource("com.lingring.domain.referral.api.ReferralControllerTest#failures")
        @DisplayName("실패 사유별로 HTTP 상태와 code를 반환한다")
        void redeem_whenFails_returnsStatusAndCode(final RuntimeException exception, final ErrorCode errorCode)
                throws Exception {
            // given
            AuthContext.set(USER_ID);
            given(referralFacade.redeem(eq(USER_ID), any())).willThrow(exception);

            // when
            final MockHttpServletResponse response = perform(new RedeemReferralRequest("추천인"));

            // then
            assertThat(response.getStatus()).isEqualTo(errorCode.getHttpStatus().value());
            final JsonNode body = objectMapper.readTree(response.getContentAsString());
            assertThat(body.get("code").asText()).isEqualTo(errorCode.name());
        }
    }

    static Stream<Arguments> failures() {
        return Stream.of(
                Arguments.of(new ReferralAlreadyRedeemedException(USER_ID), ErrorCode.REFERRAL_ALREADY_REDEEMED),
                Arguments.of(new ReferralPeriodExpiredException(USER_ID), ErrorCode.REFERRAL_PERIOD_EXPIRED),
                Arguments.of(new ReferralRejoinedUserException(USER_ID), ErrorCode.REFERRAL_NOT_ELIGIBLE_REJOINED),
                Arguments.of(
                        new NotFoundException(ErrorCode.REFERRER_NOT_FOUND, "test"),
                        ErrorCode.REFERRER_NOT_FOUND
                ),
                Arguments.of(new ReferralSelfNotAllowedException(USER_ID), ErrorCode.REFERRAL_SELF_NOT_ALLOWED)
        );
    }

    private MockHttpServletResponse perform(final RedeemReferralRequest request) throws Exception {
        return mockMvc.perform(post("/api/v1/me/referral/redeem")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andReturn()
                .getResponse();
    }
}
