package com.lingring.domain.useragreement.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.lingring.domain.user.dao.UserRepository;
import com.lingring.domain.user.domain.Provider;
import com.lingring.domain.user.domain.User;
import com.lingring.domain.user.domain.vo.Name;
import com.lingring.domain.useragreement.domain.AgreementItem;
import com.lingring.domain.useragreement.dto.request.AgreementCreateRequest;
import com.lingring.domain.useragreement.dto.response.AgreementResponse;
import com.lingring.global.config.ServiceIntegrationHelper;
import com.lingring.global.error.ErrorCode;
import com.lingring.global.error.exception.BadRequestException;
import java.util.EnumSet;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class UserAgreementServiceTest extends ServiceIntegrationHelper {

    @Autowired
    private UserAgreementService userAgreementService;

    @Autowired
    private UserRepository userRepository;

    private User seedUser() {
        return userRepository.save(
                User.createFromOAuth(Provider.KAKAO, "kakao-sub-1", new Name("링링이"), null)
        );
    }

    @Nested
    @DisplayName("accept: 약관 동의 처리")
    class Accept {

        @Test
        @DisplayName("필수 4개 항목이 모두 포함되면 agreedAt·termsVersion이 기록되고 requiresOnboarding=false")
        void accept_whenAllRequiredItems_marksAgreed() {
            // given
            final User user = seedUser();
            final AgreementCreateRequest request = new AgreementCreateRequest(
                    "2026-05-06",
                    EnumSet.allOf(AgreementItem.class)
            );

            // when
            final AgreementResponse response = userAgreementService.accept(user.getId(), request);

            // then
            assertThat(response.user().id()).isEqualTo(user.getId());
            assertThat(response.user().requiresOnboarding()).isFalse();
            final User reloaded = userRepository.findById(user.getId()).orElseThrow();
            assertThat(reloaded.getAgreedAt()).isNotNull();
            assertThat(reloaded.getAgreedTermsVersion()).isEqualTo("2026-05-06");
            assertThat(reloaded.requiresOnboarding()).isFalse();
        }

        @Test
        @DisplayName("필수 항목 중 하나라도 빠지면 AGREEMENT_ITEMS_INCOMPLETE 예외")
        void accept_whenItemMissing_throwsBadRequest() {
            // given
            final User user = seedUser();
            final AgreementCreateRequest request = new AgreementCreateRequest(
                    "2026-05-06",
                    EnumSet.of(AgreementItem.OVER14, AgreementItem.TERMS, AgreementItem.PRIVACY)
            );

            // when & then
            assertThatThrownBy(() -> userAgreementService.accept(user.getId(), request))
                    .isInstanceOf(BadRequestException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.AGREEMENT_ITEMS_INCOMPLETE);
        }

        @Test
        @DisplayName("이미 동의한 사용자가 다시 호출하면 termsVersion·agreedAt이 갱신된다")
        void accept_whenCalledTwice_updatesVersion() throws InterruptedException {
            // given
            final User user = seedUser();
            final AgreementCreateRequest first = new AgreementCreateRequest(
                    "2026-05-06",
                    EnumSet.allOf(AgreementItem.class)
            );
            userAgreementService.accept(user.getId(), first);
            final var firstAgreedAt = userRepository.findById(user.getId()).orElseThrow().getAgreedAt();
            Thread.sleep(10);

            final AgreementCreateRequest second = new AgreementCreateRequest(
                    "2026-09-01",
                    EnumSet.allOf(AgreementItem.class)
            );

            // when
            userAgreementService.accept(user.getId(), second);

            // then
            final User reloaded = userRepository.findById(user.getId()).orElseThrow();
            assertThat(reloaded.getAgreedTermsVersion()).isEqualTo("2026-09-01");
            assertThat(reloaded.getAgreedAt()).isAfter(firstAgreedAt);
        }
    }
}
