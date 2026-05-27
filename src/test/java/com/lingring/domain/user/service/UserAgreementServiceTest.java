package com.lingring.domain.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.lingring.domain.user.dao.UserRepository;
import com.lingring.domain.user.domain.Provider;
import com.lingring.domain.user.domain.User;
import com.lingring.domain.user.domain.vo.Name;
import com.lingring.domain.user.dto.request.request.AgreementCreateRequest;
import com.lingring.domain.user.dto.response.AgreementResponse;
import com.lingring.global.config.ServiceIntegrationHelper;
import com.lingring.global.error.ErrorCode;
import com.lingring.global.error.exception.BadRequestException;
import java.util.Set;
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
                    Set.of("OVER14", "TERMS", "PRIVACY", "VOICE_AI")
            );

            // when
            final AgreementResponse response = userAgreementService.accept(user.getId(), request);

            // then
            assertThat(response.user().id()).isEqualTo(user.getId());
            assertThat(response.user().requiresOnboarding()).isFalse();
            final User reloaded = userRepository.findById(user.getId()).orElseThrow();
            assertThat(reloaded.getAgreement()).isNotNull();
            assertThat(reloaded.getAgreement().getAgreedAt()).isNotNull();
            assertThat(reloaded.getAgreement().getTermsVersion()).isEqualTo("2026-05-06");
            assertThat(reloaded.requiresOnboarding()).isFalse();
        }

        @Test
        @DisplayName("필수 항목 중 하나라도 빠지면 AGREEMENT_ITEMS_INCOMPLETE 예외")
        void accept_whenItemMissing_throwsBadRequest() {
            // given
            final User user = seedUser();
            final AgreementCreateRequest request = new AgreementCreateRequest(
                    "2026-05-06",
                    Set.of("OVER14", "TERMS")
            );

            // when & then
            assertThatThrownBy(() -> userAgreementService.accept(user.getId(), request))
                    .isInstanceOf(BadRequestException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.AGREEMENT_ITEMS_INCOMPLETE);
        }

        @Test
        @DisplayName("VOICE_AI는 임시 비활성화 상태라 생략해도 정상 처리된다")
        void accept_whenVoiceAiOmitted_marksAgreed() {
            // given
            final User user = seedUser();
            final AgreementCreateRequest request = new AgreementCreateRequest(
                    "2026-05-06",
                    Set.of("OVER14", "TERMS", "PRIVACY")
            );

            // when
            userAgreementService.accept(user.getId(), request);

            // then
            final User reloaded = userRepository.findById(user.getId()).orElseThrow();
            assertThat(reloaded.requiresOnboarding()).isFalse();
        }

        @Test
        @DisplayName("이미 동의한 사용자가 다시 호출하면 termsVersion·agreedAt이 갱신된다")
        void accept_whenCalledTwice_updatesVersion() throws InterruptedException {
            // given
            final User user = seedUser();
            final AgreementCreateRequest first = new AgreementCreateRequest(
                    "2026-05-06",
                    Set.of("OVER14", "TERMS", "PRIVACY", "VOICE_AI")
            );
            userAgreementService.accept(user.getId(), first);
            final var firstAgreedAt = userRepository.findById(user.getId()).orElseThrow()
                    .getAgreement().getAgreedAt();
            Thread.sleep(10);

            final AgreementCreateRequest second = new AgreementCreateRequest(
                    "2026-09-01",
                    Set.of("OVER14", "TERMS", "PRIVACY", "VOICE_AI")
            );

            // when
            userAgreementService.accept(user.getId(), second);

            // then
            final User reloaded = userRepository.findById(user.getId()).orElseThrow();
            assertThat(reloaded.getAgreement().getTermsVersion()).isEqualTo("2026-09-01");
            assertThat(reloaded.getAgreement().getAgreedAt()).isAfter(firstAgreedAt);
        }

        @Test
        @DisplayName("agreedItems가 소문자로 들어와도 대소문자 무관하게 매핑되어 정상 처리된다")
        void accept_whenLowerCaseItems_marksAgreed() {
            // given
            final User user = seedUser();
            final AgreementCreateRequest request = new AgreementCreateRequest(
                    "2026-05-06",
                    Set.of("over14", "terms", "privacy", "voice_ai")
            );

            // when
            userAgreementService.accept(user.getId(), request);

            // then
            final User reloaded = userRepository.findById(user.getId()).orElseThrow();
            assertThat(reloaded.requiresOnboarding()).isFalse();
        }

        @Test
        @DisplayName("agreedItems가 camelCase로 들어와도 SNAKE_CASE enum으로 정상 매핑된다")
        void accept_whenCamelCaseItems_marksAgreed() {
            // given
            final User user = seedUser();
            final AgreementCreateRequest request = new AgreementCreateRequest(
                    "2026-05-06",
                    Set.of("over14", "terms", "privacy", "voiceAi")
            );

            // when
            userAgreementService.accept(user.getId(), request);

            // then
            final User reloaded = userRepository.findById(user.getId()).orElseThrow();
            assertThat(reloaded.requiresOnboarding()).isFalse();
        }

        @Test
        @DisplayName("지원하지 않는 약관 항목이 포함되면 INVALID_INPUT_VALUE 예외")
        void accept_whenUnknownItem_throwsBadRequest() {
            // given
            final User user = seedUser();
            final AgreementCreateRequest request = new AgreementCreateRequest(
                    "2026-05-06",
                    Set.of("OVER14", "TERMS", "PRIVACY", "VOICE_AI", "UNKNOWN_ITEM")
            );

            // when & then
            assertThatThrownBy(() -> userAgreementService.accept(user.getId(), request))
                    .isInstanceOf(BadRequestException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.INVALID_INPUT_VALUE);
        }
    }
}
