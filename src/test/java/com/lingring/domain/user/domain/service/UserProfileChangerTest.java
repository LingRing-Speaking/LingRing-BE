package com.lingring.domain.user.domain.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.lingring.domain.user.dao.UserRepository;
import com.lingring.domain.user.domain.Provider;
import com.lingring.domain.user.domain.User;
import com.lingring.domain.user.domain.vo.Name;
import com.lingring.domain.user.exception.NicknameConflictException;
import com.lingring.global.config.ServiceIntegrationHelper;
import com.lingring.global.error.ErrorCode;
import com.lingring.global.error.exception.BadRequestException;
import com.lingring.global.error.exception.ForbiddenException;
import com.lingring.infrastructure.s3.FakeProfileImageStorage;
import com.lingring.infrastructure.s3.FakeProfileImageStorageConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

@Import(FakeProfileImageStorageConfig.class)
class UserProfileChangerTest extends ServiceIntegrationHelper {

    @Autowired
    private UserProfileChanger userProfileChanger;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private FakeProfileImageStorage fakeProfileImageStorage;

    @BeforeEach
    void clearFakeStorage() {
        fakeProfileImageStorage.clear();
    }

    @Nested
    @DisplayName("requireUniqueName: 닉네임 중복 검증")
    class RequireUniqueName {

        @Test
        @DisplayName("중복이 없으면 통과한다")
        void requireUniqueName_whenUnique_passes() {
            // when & then
            assertThatCode(() -> userProfileChanger.requireUniqueName(new Name("새이름")))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("이미 사용 중이면 NicknameConflictException이 발생한다")
        void requireUniqueName_whenTaken_throws() {
            // given
            userRepository.save(
                    User.createFromOAuth(Provider.KAKAO, "sub-existing", new Name("이미사용중"), null)
            );

            // when & then
            assertThatThrownBy(() -> userProfileChanger.requireUniqueName(new Name("이미사용중")))
                    .isInstanceOf(NicknameConflictException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.NICKNAME_CONFLICT);
        }
    }

    @Nested
    @DisplayName("applyUpdates: 부분 갱신 흐름 결정")
    class ApplyUpdates {

        @Test
        @DisplayName("nickname만 주어지면 닉네임만 변경되고 profileImage는 그대로다")
        void applyUpdates_whenNicknameOnly_changesNicknameOnly() {
            // given
            final User user = userRepository.save(
                    User.createFromOAuth(Provider.KAKAO, "sub-nick-only", new Name("기존이름"), "https://orig/img.png")
            );

            // when
            userProfileChanger.applyUpdates(user, user.getId(), "새이름", null);

            // then
            assertThat(user.getName().getValue()).isEqualTo("새이름");
            assertThat(user.getProfileImage().getValue()).isEqualTo("https://orig/img.png");
        }

        @Test
        @DisplayName("profileImageKey만 주어지면 이미지 URL만 변경되고 nickname은 그대로다")
        void applyUpdates_whenImageKeyOnly_changesImageOnly() {
            // given
            final User user = userRepository.save(
                    User.createFromOAuth(Provider.KAKAO, "sub-img-only", new Name("그대로"), null)
            );
            final String key = "profile-images/" + user.getId() + "/abc";
            fakeProfileImageStorage.simulateUpload(key);

            // when
            userProfileChanger.applyUpdates(user, user.getId(), null, key);

            // then
            assertThat(user.getName().getValue()).isEqualTo("그대로");
            assertThat(user.getProfileImage().getValue())
                    .isEqualTo(fakeProfileImageStorage.publicUrl(key));
        }

        @Test
        @DisplayName("둘 다 공백이면 아무것도 변경되지 않는다")
        void applyUpdates_whenBothBlank_doesNothing() {
            // given
            final User user = userRepository.save(
                    User.createFromOAuth(Provider.KAKAO, "sub-noop", new Name("원래이름"), "https://orig/img.png")
            );

            // when
            userProfileChanger.applyUpdates(user, user.getId(), "   ", "");

            // then
            assertThat(user.getName().getValue()).isEqualTo("원래이름");
            assertThat(user.getProfileImage().getValue()).isEqualTo("https://orig/img.png");
        }

        @Test
        @DisplayName("다른 user prefix의 키를 주면 ForbiddenException이 발생한다")
        void applyUpdates_whenForeignKey_throws() {
            // given
            final User user = userRepository.save(
                    User.createFromOAuth(Provider.KAKAO, "sub-foreign", new Name("나야나"), null)
            );
            final Long otherId = user.getId() + 999L;
            final String foreignKey = "profile-images/" + otherId + "/xyz";
            fakeProfileImageStorage.simulateUpload(foreignKey);

            // when & then
            assertThatThrownBy(() -> userProfileChanger.applyUpdates(user, user.getId(), null, foreignKey))
                    .isInstanceOf(ForbiddenException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.PROFILE_IMAGE_KEY_FORBIDDEN);
        }

        @Test
        @DisplayName("업로드되지 않은 키를 주면 IMAGE_NOT_UPLOADED 예외가 발생한다")
        void applyUpdates_whenKeyNotUploaded_throws() {
            // given
            final User user = userRepository.save(
                    User.createFromOAuth(Provider.KAKAO, "sub-missing-img", new Name("없어요"), null)
            );
            final String key = "profile-images/" + user.getId() + "/missing";

            // when & then
            assertThatThrownBy(() -> userProfileChanger.applyUpdates(user, user.getId(), null, key))
                    .isInstanceOf(BadRequestException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.IMAGE_NOT_UPLOADED);
        }
    }
}
