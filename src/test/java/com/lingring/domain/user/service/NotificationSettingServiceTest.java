package com.lingring.domain.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.lingring.domain.user.dao.UserRepository;
import com.lingring.domain.user.domain.Provider;
import com.lingring.domain.user.domain.User;
import com.lingring.domain.user.domain.vo.Name;
import com.lingring.domain.user.dto.request.NotificationSettingUpdateRequest;
import com.lingring.domain.user.dto.response.NotificationSettingResponse;
import com.lingring.global.config.ServiceIntegrationHelper;
import com.lingring.global.error.ErrorCode;
import com.lingring.global.error.exception.NotFoundException;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class NotificationSettingServiceTest extends ServiceIntegrationHelper {

    @Autowired
    private NotificationSettingService notificationSettingService;

    @Autowired
    private UserRepository userRepository;

    private User seedUser() {
        return userRepository.save(
                User.createFromOAuth(Provider.KAKAO, "kakao-sub-1", new Name("링링이"), null)
        );
    }

    @Nested
    @DisplayName("update: 마케팅 푸시 수신 동의 변경")
    class Update {

        @Test
        @DisplayName("true를 보내면 수신 동의와 처리 시각이 저장되고 갱신된 user를 반환한다")
        void update_whenAgree_savesConsent() {
            // given
            final User user = seedUser();

            // when
            final NotificationSettingResponse response = notificationSettingService.update(
                    user.getId(),
                    new NotificationSettingUpdateRequest(true)
            );

            // then
            assertThat(response.user().id()).isEqualTo(user.getId());
            assertThat(response.user().marketingPushAgreed()).isTrue();
            assertThat(response.user().marketingPushUpdatedAt()).isNotNull();
            final User reloaded = userRepository.findById(user.getId()).orElseThrow();
            assertThat(reloaded.getMarketingPushConsent().isAgreed()).isTrue();
        }

        @Test
        @DisplayName("동의 상태에서 false를 보내면 철회되고 처리 시각이 남는다")
        void update_whenWithdraw_savesWithdrawal() {
            // given
            final User user = seedUser();
            notificationSettingService.update(user.getId(), new NotificationSettingUpdateRequest(true));

            // when
            final NotificationSettingResponse response = notificationSettingService.update(
                    user.getId(),
                    new NotificationSettingUpdateRequest(false)
            );

            // then
            assertThat(response.user().marketingPushAgreed()).isFalse();
            assertThat(response.user().marketingPushUpdatedAt()).isNotNull();
        }

        @Test
        @DisplayName("이미 동의한 상태에서 true를 다시 보내면 성공하고 처리 시각을 유지한다")
        void update_whenSameValue_keepsUpdatedAt() {
            // given
            final User user = seedUser();
            final LocalDateTime firstUpdatedAt = notificationSettingService.update(
                    user.getId(),
                    new NotificationSettingUpdateRequest(true)
            ).user().marketingPushUpdatedAt();

            // when
            final NotificationSettingResponse response = notificationSettingService.update(
                    user.getId(),
                    new NotificationSettingUpdateRequest(true)
            );

            // then
            assertThat(response.user().marketingPushUpdatedAt()).isEqualTo(firstUpdatedAt);
        }

        @Test
        @DisplayName("존재하지 않는 사용자면 USER_NOT_FOUND 예외")
        void update_whenUserMissing_throwsNotFound() {
            // when & then
            assertThatThrownBy(() -> notificationSettingService.update(
                    999L,
                    new NotificationSettingUpdateRequest(true)
            ))
                    .isInstanceOf(NotFoundException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.USER_NOT_FOUND);
        }
    }
}
