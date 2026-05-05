package com.lingring.domain.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.lingring.domain.user.dao.UserRepository;
import com.lingring.domain.user.dao.UserStatsRepository;
import com.lingring.domain.user.domain.Provider;
import com.lingring.domain.user.domain.User;
import com.lingring.domain.user.domain.UserStats;
import com.lingring.domain.user.domain.vo.Name;
import com.lingring.domain.user.dto.request.PresignedUrlRequest;
import com.lingring.domain.user.dto.request.UpdateProfileRequest;
import com.lingring.domain.user.dto.response.MeResponse;
import com.lingring.domain.user.dto.response.PresignedUrlResponse;
import com.lingring.domain.user.dto.response.UpdateProfileResponse;
import com.lingring.domain.user.dto.response.UserProfileResponse;
import com.lingring.domain.user.exception.NicknameConflictException;
import com.lingring.global.config.ServiceIntegrationHelper;
import com.lingring.global.error.ErrorCode;
import com.lingring.global.error.exception.BadRequestException;
import com.lingring.global.error.exception.ForbiddenException;
import com.lingring.global.error.exception.NotFoundException;
import com.lingring.global.error.exception.UnauthorizedException;
import com.lingring.infrastructure.s3.FakeProfileImageStorage;
import com.lingring.infrastructure.s3.FakeProfileImageStorageConfig;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

@Import(FakeProfileImageStorageConfig.class)
class UserServiceTest extends ServiceIntegrationHelper {

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserStatsRepository userStatsRepository;

    @Autowired
    private FakeProfileImageStorage fakeProfileImageStorage;

    @BeforeEach
    void clearFakeStorage() {
        fakeProfileImageStorage.clear();
    }

    @Nested
    @DisplayName("getMe: 인증된 사용자 본인 정보 조회")
    class GetMe {

        @Test
        @DisplayName("존재하는 사용자 id로 조회하면 id와 nickname을 반환한다")
        void getMe_whenUserExists_returnsResponse() {
            // given
            final User saved = userRepository.save(
                    User.createFromOAuth(Provider.KAKAO, "kakao-test-sub-1", new Name("링링"), null)
            );

            // when
            final MeResponse response = userService.getMe(saved.getId());

            // then
            assertThat(response.id()).isEqualTo(saved.getId());
            assertThat(response.nickname()).isEqualTo("링링");
        }

        @Test
        @DisplayName("존재하지 않는 사용자 id로 조회하면 INVALID_TOKEN 예외가 발생한다 (401)")
        void getMe_whenUserNotFound_throwsUnauthorized() {
            // given
            final Long missingId = 9_999_999L;

            // when & then
            assertThatThrownBy(() -> userService.getMe(missingId))
                    .isInstanceOf(UnauthorizedException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.INVALID_TOKEN);
        }
    }

    @Nested
    @DisplayName("getUserProfile: userId로 타인 프로필 조회")
    class GetUserProfile {

        @Test
        @DisplayName("user와 stats가 모두 존재하면 id, nickname, level, mannerTemperature를 반환한다")
        void getUserProfile_whenUserAndStatsExist_returnsResponse() {
            // given
            final User saved = userRepository.save(
                    User.createFromOAuth(Provider.KAKAO, "kakao-sub-profile", new Name("Sophie"), null)
            );
            userStatsRepository.save(UserStats.create(saved.getId()));

            // when
            final UserProfileResponse response = userService.getUserProfile(saved.getId());

            // then
            assertThat(response.id()).isEqualTo(saved.getId());
            assertThat(response.nickname()).isEqualTo("Sophie");
            assertThat(response.level()).isNotNull();
            assertThat(response.mannerTemperature()).isNotNull();
        }

        @Test
        @DisplayName("user가 존재하지 않으면 USER_NOT_FOUND 예외가 발생한다 (404)")
        void getUserProfile_whenUserNotFound_throwsNotFound() {
            // given
            final Long missingId = 9_999_999L;

            // when & then
            assertThatThrownBy(() -> userService.getUserProfile(missingId))
                    .isInstanceOf(NotFoundException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.USER_NOT_FOUND);
        }

        @Test
        @DisplayName("user는 있지만 stats가 없으면 USER_NOT_FOUND 예외가 발생한다 (404)")
        void getUserProfile_whenStatsMissing_throwsUserNotFound() {
            // given
            final User saved = userRepository.save(
                    User.createFromOAuth(Provider.KAKAO, "kakao-sub-no-stats", new Name("NoStats"), null)
            );

            // when & then
            assertThatThrownBy(() -> userService.getUserProfile(saved.getId()))
                    .isInstanceOf(NotFoundException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.USER_NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("findByProvider: provider + providerUserId 로 사용자 조회")
    class FindByProvider {

        @Test
        @DisplayName("일치하는 사용자가 있으면 Optional<User>을 반환한다")
        void findByProvider_whenExists_returnsUser() {
            // given
            final User saved = userRepository.save(
                    User.createFromOAuth(Provider.KAKAO, "sub-1", new Name("링링"), null)
            );

            // when
            final Optional<User> found = userService.findByProvider(Provider.KAKAO, "sub-1");

            // then
            assertThat(found).isPresent();
            assertThat(found.get().getId()).isEqualTo(saved.getId());
        }

        @Test
        @DisplayName("일치하는 사용자가 없으면 Optional.empty를 반환한다")
        void findByProvider_whenNotExists_returnsEmpty() {
            // when
            final Optional<User> found = userService.findByProvider(Provider.KAKAO, "no-such-sub");

            // then
            assertThat(found).isEmpty();
        }

        @Test
        @DisplayName("provider가 다르면 매칭되지 않는다")
        void findByProvider_whenDifferentProvider_returnsEmpty() {
            // given — 카카오로 저장
            userRepository.save(
                    User.createFromOAuth(Provider.KAKAO, "shared-sub", new Name("링링"), null)
            );

            // when — 같은 sub지만 애플로 조회
            final Optional<User> found = userService.findByProvider(Provider.APPLE, "shared-sub");

            // then
            assertThat(found).isEmpty();
        }
    }

    @Nested
    @DisplayName("register: 신규 사용자 가입")
    class Register {

        @Test
        @DisplayName("유효한 입력이면 User를 저장하고 UserStats도 함께 생성한다")
        void register_whenValid_savesUserAndUserStats() {
            // when
            final User user = userService.register(Provider.KAKAO, "new-sub", "링링이");

            // then
            assertThat(user.getId()).isNotNull();
            assertThat(user.getProvider()).isEqualTo(Provider.KAKAO);
            assertThat(user.getProviderUserId()).isEqualTo("new-sub");
            assertThat(user.getName().getValue()).isEqualTo("링링이");
            assertThat(userStatsRepository.findByUserId(user.getId())).isPresent();
        }

        @Test
        @DisplayName("nickname이 null이면 NICKNAME_REQUIRED 예외가 발생한다")
        void register_whenNicknameNull_throwsNicknameRequired() {
            // when & then
            assertThatThrownBy(() -> userService.register(Provider.KAKAO, "sub-1", null))
                    .isInstanceOf(BadRequestException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.NICKNAME_REQUIRED);
        }

        @Test
        @DisplayName("nickname이 공백이면 NICKNAME_REQUIRED 예외가 발생한다")
        void register_whenNicknameBlank_throwsNicknameRequired() {
            // when & then
            assertThatThrownBy(() -> userService.register(Provider.KAKAO, "sub-1", "   "))
                    .isInstanceOf(BadRequestException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.NICKNAME_REQUIRED);
        }

        @Test
        @DisplayName("이미 사용 중인 nickname이면 NICKNAME_CONFLICT 예외가 발생한다")
        void register_whenNicknameAlreadyTaken_throwsNicknameConflict() {
            // given
            userRepository.save(
                    User.createFromOAuth(Provider.KAKAO, "existing-sub", new Name("링링이"), null)
            );

            // when & then
            assertThatThrownBy(() -> userService.register(Provider.APPLE, "new-sub", "링링이"))
                    .isInstanceOf(NicknameConflictException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.NICKNAME_CONFLICT);
        }
    }

    @Nested
    @DisplayName("createProfileImageUploadUrl: 프로필 이미지 presigned URL 발급")
    class CreateProfileImageUploadUrl {

        @Test
        @DisplayName("유효한 요청이면 본인 user prefix가 포함된 키와 업로드 URL을 반환한다")
        void createProfileImageUploadUrl_whenValid_returnsResponse() {
            // given
            final Long userId = 1L;
            final PresignedUrlRequest request = new PresignedUrlRequest("image/png", 12345L);

            // when
            final PresignedUrlResponse response = userService.createProfileImageUploadUrl(userId, request);

            // then
            assertThat(response.key()).startsWith("profile-images/" + userId + "/");
            assertThat(response.uploadUrl()).contains(response.key());
        }

        @Test
        @DisplayName("contentType이 image/로 시작하지 않으면 INVALID_IMAGE_CONTENT_TYPE 예외가 발생한다")
        void createProfileImageUploadUrl_whenNotImage_throws() {
            // given
            final PresignedUrlRequest request = new PresignedUrlRequest("application/pdf", 1234L);

            // when & then
            assertThatThrownBy(() -> userService.createProfileImageUploadUrl(1L, request))
                    .isInstanceOf(BadRequestException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.INVALID_IMAGE_CONTENT_TYPE);
        }

        @Test
        @DisplayName("contentLength가 max를 초과하면 IMAGE_TOO_LARGE 예외가 발생한다")
        void createProfileImageUploadUrl_whenTooLarge_throws() {
            // given — 5MB + 1
            final PresignedUrlRequest request = new PresignedUrlRequest("image/png", 5_242_881L);

            // when & then
            assertThatThrownBy(() -> userService.createProfileImageUploadUrl(1L, request))
                    .isInstanceOf(BadRequestException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.IMAGE_TOO_LARGE);
        }
    }

    @Nested
    @DisplayName("updateProfile: 프로필 변경")
    class UpdateProfile {

        @Test
        @DisplayName("nickname만 보내면 닉네임만 변경되고 profileImage는 그대로다")
        void updateProfile_whenNicknameOnly_changesNicknameOnly() {
            // given
            final User saved = userRepository.save(
                    User.createFromOAuth(Provider.KAKAO, "sub-nickname-only", new Name("기존이름"), "https://orig/img.png")
            );

            // when
            final UpdateProfileResponse response = userService.updateProfile(
                    saved.getId(), new UpdateProfileRequest("새이름", null)
            );

            // then
            assertThat(response.nickname()).isEqualTo("새이름");
            assertThat(response.profileImage()).isEqualTo("https://orig/img.png");
        }

        @Test
        @DisplayName("이미 사용 중인 nickname이면 NICKNAME_CONFLICT 예외가 발생한다")
        void updateProfile_whenNicknameTaken_throws() {
            // given
            final User saved = userRepository.save(
                    User.createFromOAuth(Provider.KAKAO, "sub-mine", new Name("내이름"), null)
            );
            userRepository.save(
                    User.createFromOAuth(Provider.APPLE, "sub-other", new Name("이미사용중"), null)
            );

            // when & then
            assertThatThrownBy(() -> userService.updateProfile(
                    saved.getId(), new UpdateProfileRequest("이미사용중", null)
            )).isInstanceOf(NicknameConflictException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.NICKNAME_CONFLICT);
        }

        @Test
        @DisplayName("profileImageKey만 보내면 이미지 URL만 변경되고 nickname은 그대로다")
        void updateProfile_whenImageKeyOnly_changesImageOnly() {
            // given
            final User saved = userRepository.save(
                    User.createFromOAuth(Provider.KAKAO, "sub-image-only", new Name("그대로"), null)
            );
            final String key = "profile-images/" + saved.getId() + "/abc123";
            fakeProfileImageStorage.simulateUpload(key);

            // when
            final UpdateProfileResponse response = userService.updateProfile(
                    saved.getId(), new UpdateProfileRequest(null, key)
            );

            // then
            assertThat(response.nickname()).isEqualTo("그대로");
            assertThat(response.profileImage()).isEqualTo(fakeProfileImageStorage.publicUrl(key));
        }

        @Test
        @DisplayName("다른 사용자 prefix의 키를 보내면 PROFILE_IMAGE_KEY_FORBIDDEN 예외가 발생한다")
        void updateProfile_whenForeignKey_throws() {
            // given
            final User saved = userRepository.save(
                    User.createFromOAuth(Provider.KAKAO, "sub-foreign", new Name("나야나"), null)
            );
            final Long otherUserId = saved.getId() + 999L;
            final String foreignKey = "profile-images/" + otherUserId + "/xyz";
            fakeProfileImageStorage.simulateUpload(foreignKey);

            // when & then
            assertThatThrownBy(() -> userService.updateProfile(
                    saved.getId(), new UpdateProfileRequest(null, foreignKey)
            )).isInstanceOf(ForbiddenException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.PROFILE_IMAGE_KEY_FORBIDDEN);
        }

        @Test
        @DisplayName("업로드되지 않은 키를 보내면 IMAGE_NOT_UPLOADED 예외가 발생한다")
        void updateProfile_whenKeyNotUploaded_throws() {
            // given
            final User saved = userRepository.save(
                    User.createFromOAuth(Provider.KAKAO, "sub-missing", new Name("없어요"), null)
            );
            final String key = "profile-images/" + saved.getId() + "/missing";

            // when & then
            assertThatThrownBy(() -> userService.updateProfile(
                    saved.getId(), new UpdateProfileRequest(null, key)
            )).isInstanceOf(BadRequestException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.IMAGE_NOT_UPLOADED);
        }

        @Test
        @DisplayName("nickname과 profileImageKey 둘 다 보내면 둘 다 변경된다")
        void updateProfile_whenBoth_changesBoth() {
            // given
            final User saved = userRepository.save(
                    User.createFromOAuth(Provider.KAKAO, "sub-both", new Name("이전이름"), "https://orig/img.png")
            );
            final String key = "profile-images/" + saved.getId() + "/new-key";
            fakeProfileImageStorage.simulateUpload(key);

            // when
            final UpdateProfileResponse response = userService.updateProfile(
                    saved.getId(), new UpdateProfileRequest("새이름", key)
            );

            // then
            assertThat(response.nickname()).isEqualTo("새이름");
            assertThat(response.profileImage()).isEqualTo(fakeProfileImageStorage.publicUrl(key));
        }

        @Test
        @DisplayName("nickname과 profileImageKey가 모두 비어있으면 EMPTY_UPDATE_PROFILE_REQUEST 예외가 발생한다")
        void updateProfile_whenAllEmpty_throws() {
            // when & then
            assertThatThrownBy(() -> userService.updateProfile(
                    1L, new UpdateProfileRequest(null, null)
            )).isInstanceOf(BadRequestException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.EMPTY_UPDATE_PROFILE_REQUEST);
        }

        @Test
        @DisplayName("존재하지 않는 사용자 id면 USER_NOT_FOUND 예외가 발생한다")
        void updateProfile_whenUserNotFound_throws() {
            // given
            final Long missingId = 9_999_999L;

            // when & then
            assertThatThrownBy(() -> userService.updateProfile(
                    missingId, new UpdateProfileRequest("새이름", null)
            )).isInstanceOf(NotFoundException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.USER_NOT_FOUND);
        }
    }
}
