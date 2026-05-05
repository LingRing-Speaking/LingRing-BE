package com.lingring.domain.user.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.only;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import com.lingring.domain.user.domain.Level;
import com.lingring.domain.user.domain.WithdrawReason;
import com.lingring.domain.user.dto.request.PresignedUrlRequest;
import com.lingring.domain.user.dto.request.UpdateProfileRequest;
import com.lingring.domain.user.dto.request.WithdrawRequest;
import com.lingring.domain.user.dto.response.MeResponse;
import com.lingring.domain.user.dto.response.PresignedUrlResponse;
import com.lingring.domain.user.dto.response.UpdateProfileResponse;
import com.lingring.domain.user.dto.response.UserProfileResponse;
import com.lingring.domain.user.facade.UserWithdrawalFacade;
import com.lingring.domain.user.service.UserService;
import com.lingring.global.auth.context.AuthContext;
import com.lingring.global.error.ErrorCode;
import com.lingring.global.error.exception.BadRequestException;
import com.lingring.global.error.exception.ForbiddenException;
import com.lingring.global.error.exception.NotFoundException;
import com.lingring.global.error.exception.UnauthorizedException;
import java.math.BigDecimal;
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

@WebMvcTest(UserController.class)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private UserWithdrawalFacade userWithdrawalFacade;

    @AfterEach
    void clearAuthContext() {
        AuthContext.clear();
    }

    @Nested
    @DisplayName("GET /me")
    class GetMe {

        @Test
        @DisplayName("사용자가 존재하면 200 응답과 id, nickname, profileImage를 반환한다")
        void getMe_whenUserExists_returns200WithBody() throws Exception {
            // given
            final Long userId = 1L;
            final String profileImageUrl = "https://lingring-dev.s3.ap-northeast-2.amazonaws.com/profile-images/1/abc.jpg";
            AuthContext.set(userId);
            given(userService.getMe(userId)).willReturn(new MeResponse(userId, "링링", profileImageUrl));

            // when
            final MockHttpServletResponse response = mockMvc.perform(get("/api/v1/me")
                            .accept(MediaType.APPLICATION_JSON))
                    .andReturn()
                    .getResponse();

            // then
            assertThat(response.getStatus()).isEqualTo(200);
            final JsonNode body = objectMapper.readTree(response.getContentAsString());
            assertThat(body.get("status").asInt()).isEqualTo(200);
            assertThat(body.get("data").get("id").asLong()).isEqualTo(userId);
            assertThat(body.get("data").get("nickname").asText()).isEqualTo("링링");
            assertThat(body.get("data").get("profileImage").asText()).isEqualTo(profileImageUrl);
        }

        @Test
        @DisplayName("사용자가 없으면 INVALID_TOKEN 에러 메시지를 body에 담아 401로 반환한다")
        void getMe_whenUserNotFound_returnsErrorMessage() throws Exception {
            // given
            final Long userId = 999L;
            AuthContext.set(userId);
            willThrow(new UnauthorizedException(
                    ErrorCode.INVALID_TOKEN,
                    "토큰 소유자를 찾을 수 없습니다. 다시 로그인하세요."
            )).given(userService).getMe(userId);

            // when
            final MockHttpServletResponse response = mockMvc.perform(get("/api/v1/me")
                            .accept(MediaType.APPLICATION_JSON))
                    .andReturn()
                    .getResponse();

            // then
            final JsonNode body = objectMapper.readTree(response.getContentAsString());
            assertThat(body.get("status").asInt()).isEqualTo(ErrorCode.INVALID_TOKEN.getHttpStatus().value());
            assertThat(body.get("message").asText()).isEqualTo(ErrorCode.INVALID_TOKEN.getMessage());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/users/{userId}")
    class GetUserProfile {

        @Test
        @DisplayName("사용자가 존재하면 200 응답과 id, nickname, level, mannerTemperature를 반환한다")
        void getUserProfile_whenUserExists_returns200WithBody() throws Exception {
            // given
            final Long callerId = 1L;
            final Long targetId = 7L;
            final String profileImageUrl = "https://lingring-dev.s3.ap-northeast-2.amazonaws.com/profile-images/7/abc.jpg";
            AuthContext.set(callerId);
            given(userService.getUserProfile(targetId)).willReturn(new UserProfileResponse(
                    targetId, "Sophie", profileImageUrl, Level.ADVANCED, new BigDecimal("38.5")
            ));

            // when
            final MockHttpServletResponse response = mockMvc.perform(get("/api/v1/users/{userId}", targetId)
                            .accept(MediaType.APPLICATION_JSON))
                    .andReturn()
                    .getResponse();

            // then
            assertThat(response.getStatus()).isEqualTo(200);
            final JsonNode body = objectMapper.readTree(response.getContentAsString());
            assertThat(body.get("status").asInt()).isEqualTo(200);
            assertThat(body.get("data").get("id").asLong()).isEqualTo(targetId);
            assertThat(body.get("data").get("nickname").asText()).isEqualTo("Sophie");
            assertThat(body.get("data").get("profileImage").asText()).isEqualTo(profileImageUrl);
            assertThat(body.get("data").get("level").asText()).isEqualTo("ADVANCED");
            assertThat(body.get("data").get("mannerTemperature").decimalValue())
                    .isEqualByComparingTo(new BigDecimal("38.5"));
        }

        @Test
        @DisplayName("사용자가 없으면 USER_NOT_FOUND 에러 메시지를 body에 담아 404로 반환한다")
        void getUserProfile_whenUserNotFound_returnsErrorMessage() throws Exception {
            // given
            final Long callerId = 1L;
            final Long targetId = 999L;
            AuthContext.set(callerId);
            willThrow(new NotFoundException(
                    ErrorCode.USER_NOT_FOUND,
                    "ID가 %d인 사용자를 찾을 수 없습니다.".formatted(targetId)
            )).given(userService).getUserProfile(targetId);

            // when
            final MockHttpServletResponse response = mockMvc.perform(get("/api/v1/users/{userId}", targetId)
                            .accept(MediaType.APPLICATION_JSON))
                    .andReturn()
                    .getResponse();

            // then
            final JsonNode body = objectMapper.readTree(response.getContentAsString());
            assertThat(body.get("status").asInt()).isEqualTo(ErrorCode.USER_NOT_FOUND.getHttpStatus().value());
            assertThat(body.get("message").asText()).isEqualTo(ErrorCode.USER_NOT_FOUND.getMessage());
        }
    }

    @Nested
    @DisplayName("POST /me/withdraw")
    class Withdraw {

        @Test
        @DisplayName("탈퇴가 정상 처리되면 204 응답과 함께 facade.withdraw가 호출된다")
        void withdraw_whenSuccess_returns204() throws Exception {
            // given
            final Long userId = 1L;
            AuthContext.set(userId);
            final String body = objectMapper.writeValueAsString(
                    new WithdrawRequest(WithdrawReason.NO_GOOD_MATCH, null)
            );

            // when
            final MockHttpServletResponse response = mockMvc.perform(post("/api/v1/me/withdraw")
                            .contentType(MediaType.APPLICATION_JSON)
                            .accept(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andReturn()
                    .getResponse();

            // then
            assertThat(response.getStatus()).isEqualTo(204);
            then(userWithdrawalFacade).should(only()).withdraw(userId, WithdrawReason.NO_GOOD_MATCH, null);
        }

        @Test
        @DisplayName("OTHER 사유로 description을 동봉하면 204 응답과 함께 facade.withdraw가 호출된다")
        void withdraw_whenOtherWithDescription_returns204() throws Exception {
            // given
            final Long userId = 1L;
            final String description = "더 이상 사용할 일이 없어요";
            AuthContext.set(userId);
            final String body = objectMapper.writeValueAsString(
                    new WithdrawRequest(WithdrawReason.OTHER, description)
            );

            // when
            final MockHttpServletResponse response = mockMvc.perform(post("/api/v1/me/withdraw")
                            .contentType(MediaType.APPLICATION_JSON)
                            .accept(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andReturn()
                    .getResponse();

            // then
            assertThat(response.getStatus()).isEqualTo(204);
            then(userWithdrawalFacade).should(only()).withdraw(userId, WithdrawReason.OTHER, description);
        }

        @Test
        @DisplayName("OTHER 사유인데 description이 없으면 INVALID_INPUT_VALUE 에러 메시지를 body에 담아 400으로 반환한다")
        void withdraw_whenOtherWithoutDescription_returns400() throws Exception {
            // given
            final Long userId = 1L;
            AuthContext.set(userId);
            final String body = objectMapper.writeValueAsString(
                    new WithdrawRequest(WithdrawReason.OTHER, null)
            );

            // when
            final MockHttpServletResponse response = mockMvc.perform(post("/api/v1/me/withdraw")
                            .contentType(MediaType.APPLICATION_JSON)
                            .accept(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andReturn()
                    .getResponse();

            // then
            final JsonNode responseBody = objectMapper.readTree(response.getContentAsString());
            assertThat(responseBody.get("status").asInt())
                    .isEqualTo(ErrorCode.INVALID_INPUT_VALUE.getHttpStatus().value());
            assertThat(responseBody.get("message").asText())
                    .isEqualTo(ErrorCode.INVALID_INPUT_VALUE.getMessage());
            verifyNoInteractions(userWithdrawalFacade);
        }

        @Test
        @DisplayName("reason이 누락되면 INVALID_INPUT_VALUE 에러 메시지를 body에 담아 400으로 반환한다")
        void withdraw_whenReasonMissing_returns400() throws Exception {
            // given
            final Long userId = 1L;
            AuthContext.set(userId);
            final String body = "{}";

            // when
            final MockHttpServletResponse response = mockMvc.perform(post("/api/v1/me/withdraw")
                            .contentType(MediaType.APPLICATION_JSON)
                            .accept(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andReturn()
                    .getResponse();

            // then
            final JsonNode responseBody = objectMapper.readTree(response.getContentAsString());
            assertThat(responseBody.get("status").asInt())
                    .isEqualTo(ErrorCode.INVALID_INPUT_VALUE.getHttpStatus().value());
            verifyNoInteractions(userWithdrawalFacade);
        }

        @Test
        @DisplayName("사용자가 없으면 USER_NOT_FOUND 에러 메시지를 body에 담아 404로 반환한다")
        void withdraw_whenUserNotFound_returnsErrorMessage() throws Exception {
            // given
            final Long userId = 999L;
            AuthContext.set(userId);
            willThrow(new NotFoundException(
                    ErrorCode.USER_NOT_FOUND,
                    "ID가 %d인 사용자를 찾을 수 없습니다.".formatted(userId)
            )).given(userWithdrawalFacade).withdraw(userId, WithdrawReason.NO_GOOD_MATCH, null);
            final String body = objectMapper.writeValueAsString(
                    new WithdrawRequest(WithdrawReason.NO_GOOD_MATCH, null)
            );

            // when
            final MockHttpServletResponse response = mockMvc.perform(post("/api/v1/me/withdraw")
                            .contentType(MediaType.APPLICATION_JSON)
                            .accept(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andReturn()
                    .getResponse();

            // then
            final JsonNode responseBody = objectMapper.readTree(response.getContentAsString());
            assertThat(responseBody.get("status").asInt()).isEqualTo(ErrorCode.USER_NOT_FOUND.getHttpStatus().value());
            assertThat(responseBody.get("message").asText()).isEqualTo(ErrorCode.USER_NOT_FOUND.getMessage());
        }
    }

    @Nested
    @DisplayName("POST /me/profile/image/presigned-url")
    class CreateProfileImageUploadUrl {

        @Test
        @DisplayName("정상 발급되면 200 응답과 uploadUrl, key를 반환한다")
        void createProfileImageUploadUrl_whenSuccess_returns200WithBody() throws Exception {
            // given
            final Long userId = 1L;
            AuthContext.set(userId);
            final PresignedUrlRequest request = new PresignedUrlRequest("image/png", 12345L);
            given(userService.createProfileImageUploadUrl(userId, request))
                    .willReturn(new PresignedUrlResponse("https://fake-s3.local/profile-images/1/abc", "profile-images/1/abc"));

            // when
            final MockHttpServletResponse response = mockMvc.perform(post("/api/v1/me/profile/image/presigned-url")
                            .contentType(MediaType.APPLICATION_JSON)
                            .accept(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andReturn()
                    .getResponse();

            // then
            assertThat(response.getStatus()).isEqualTo(200);
            final JsonNode body = objectMapper.readTree(response.getContentAsString());
            assertThat(body.get("data").get("uploadUrl").asText())
                    .isEqualTo("https://fake-s3.local/profile-images/1/abc");
            assertThat(body.get("data").get("key").asText()).isEqualTo("profile-images/1/abc");
        }

        @Test
        @DisplayName("contentType이 null이면 INVALID_IMAGE_CONTENT_TYPE 에러로 400을 반환한다")
        void createProfileImageUploadUrl_whenContentTypeNull_returns400() throws Exception {
            // given
            final Long userId = 1L;
            AuthContext.set(userId);
            final PresignedUrlRequest request = new PresignedUrlRequest(null, 1234L);
            willThrow(new BadRequestException(
                    ErrorCode.INVALID_IMAGE_CONTENT_TYPE,
                    "허용된 이미지 형식이 아닙니다: null"
            )).given(userService).createProfileImageUploadUrl(userId, request);

            // when
            final MockHttpServletResponse response = mockMvc.perform(post("/api/v1/me/profile/image/presigned-url")
                            .contentType(MediaType.APPLICATION_JSON)
                            .accept(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andReturn()
                    .getResponse();

            // then
            final JsonNode responseBody = objectMapper.readTree(response.getContentAsString());
            assertThat(responseBody.get("status").asInt())
                    .isEqualTo(ErrorCode.INVALID_IMAGE_CONTENT_TYPE.getHttpStatus().value());
            assertThat(responseBody.get("message").asText())
                    .isEqualTo(ErrorCode.INVALID_IMAGE_CONTENT_TYPE.getMessage());
        }

        @Test
        @DisplayName("이미지가 아닌 contentType이면 400 응답과 함께 INVALID_IMAGE_CONTENT_TYPE을 반환한다")
        void createProfileImageUploadUrl_whenNotImage_returns400() throws Exception {
            // given
            final Long userId = 1L;
            AuthContext.set(userId);
            final PresignedUrlRequest request = new PresignedUrlRequest("application/pdf", 1234L);
            willThrow(new BadRequestException(
                    ErrorCode.INVALID_IMAGE_CONTENT_TYPE,
                    "허용된 이미지 형식이 아닙니다: application/pdf"
            )).given(userService).createProfileImageUploadUrl(userId, request);

            // when
            final MockHttpServletResponse response = mockMvc.perform(post("/api/v1/me/profile/image/presigned-url")
                            .contentType(MediaType.APPLICATION_JSON)
                            .accept(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andReturn()
                    .getResponse();

            // then
            final JsonNode responseBody = objectMapper.readTree(response.getContentAsString());
            assertThat(responseBody.get("status").asInt())
                    .isEqualTo(ErrorCode.INVALID_IMAGE_CONTENT_TYPE.getHttpStatus().value());
            assertThat(responseBody.get("message").asText())
                    .isEqualTo(ErrorCode.INVALID_IMAGE_CONTENT_TYPE.getMessage());
        }
    }

    @Nested
    @DisplayName("PATCH /me/profile")
    class UpdateProfile {

        @Test
        @DisplayName("정상 변경되면 200 응답과 갱신된 프로필을 반환한다")
        void updateProfile_whenSuccess_returns200WithBody() throws Exception {
            // given
            final Long userId = 1L;
            AuthContext.set(userId);
            final UpdateProfileRequest request = new UpdateProfileRequest("새이름", "profile-images/1/abc");
            given(userService.updateProfile(userId, request))
                    .willReturn(new UpdateProfileResponse(userId, "새이름", "https://fake-cdn.local/profile-images/1/abc"));

            // when
            final MockHttpServletResponse response = mockMvc.perform(patch("/api/v1/me/profile")
                            .contentType(MediaType.APPLICATION_JSON)
                            .accept(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andReturn()
                    .getResponse();

            // then
            assertThat(response.getStatus()).isEqualTo(200);
            final JsonNode body = objectMapper.readTree(response.getContentAsString());
            assertThat(body.get("data").get("id").asLong()).isEqualTo(userId);
            assertThat(body.get("data").get("nickname").asText()).isEqualTo("새이름");
            assertThat(body.get("data").get("profileImage").asText())
                    .isEqualTo("https://fake-cdn.local/profile-images/1/abc");
        }

        @Test
        @DisplayName("둘 다 비어있으면 EMPTY_UPDATE_PROFILE_REQUEST 에러로 400을 반환한다")
        void updateProfile_whenAllEmpty_returns400() throws Exception {
            // given
            final Long userId = 1L;
            AuthContext.set(userId);
            final UpdateProfileRequest request = new UpdateProfileRequest(null, null);
            willThrow(new BadRequestException(
                    ErrorCode.EMPTY_UPDATE_PROFILE_REQUEST,
                    "변경할 항목이 하나 이상 필요합니다."
            )).given(userService).updateProfile(userId, request);

            // when
            final MockHttpServletResponse response = mockMvc.perform(patch("/api/v1/me/profile")
                            .contentType(MediaType.APPLICATION_JSON)
                            .accept(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andReturn()
                    .getResponse();

            // then
            final JsonNode body = objectMapper.readTree(response.getContentAsString());
            assertThat(body.get("status").asInt())
                    .isEqualTo(ErrorCode.EMPTY_UPDATE_PROFILE_REQUEST.getHttpStatus().value());
            assertThat(body.get("message").asText())
                    .isEqualTo(ErrorCode.EMPTY_UPDATE_PROFILE_REQUEST.getMessage());
        }

        @Test
        @DisplayName("다른 사용자 prefix의 키면 PROFILE_IMAGE_KEY_FORBIDDEN 에러로 403을 반환한다")
        void updateProfile_whenForeignKey_returns403() throws Exception {
            // given
            final Long userId = 1L;
            AuthContext.set(userId);
            final UpdateProfileRequest request = new UpdateProfileRequest(null, "profile-images/9999/x");
            willThrow(new ForbiddenException(
                    ErrorCode.PROFILE_IMAGE_KEY_FORBIDDEN,
                    "본인의 프로필 이미지 키만 사용할 수 있습니다: profile-images/9999/x"
            )).given(userService).updateProfile(userId, request);

            // when
            final MockHttpServletResponse response = mockMvc.perform(patch("/api/v1/me/profile")
                            .contentType(MediaType.APPLICATION_JSON)
                            .accept(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andReturn()
                    .getResponse();

            // then
            final JsonNode body = objectMapper.readTree(response.getContentAsString());
            assertThat(body.get("status").asInt())
                    .isEqualTo(ErrorCode.PROFILE_IMAGE_KEY_FORBIDDEN.getHttpStatus().value());
        }
    }
}
