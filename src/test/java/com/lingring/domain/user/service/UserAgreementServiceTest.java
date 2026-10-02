package com.lingring.domain.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.lingring.domain.user.dao.UserRepository;
import com.lingring.domain.user.domain.Provider;
import com.lingring.domain.user.domain.User;
import com.lingring.domain.user.domain.vo.Name;
import com.lingring.domain.user.dto.request.AgreementCreateRequest;
import com.lingring.domain.user.dto.response.AgreementResponse;
import com.lingring.global.config.ServiceIntegrationHelper;
import com.lingring.global.error.ErrorCode;
import com.lingring.global.error.exception.BadRequestException;
import java.time.LocalDateTime;
import java.util.HashSet;
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
        @DisplayName("VOICE_AI가 빠지면 AGREEMENT_ITEMS_INCOMPLETE 예외 (통화 녹음·분석 필수 동의)")
        void accept_whenVoiceAiOmitted_throwsBadRequest() {
            // given
            final User user = seedUser();
            final AgreementCreateRequest request = new AgreementCreateRequest(
                    "2026-05-06",
                    Set.of("OVER14", "TERMS", "PRIVACY")
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

    @Nested
    @DisplayName("accept: 마케팅 푸시 수신 동의 (선택 항목)")
    class AcceptMarketingPush {

        private static final Set<String> REQUIRED_ITEMS = Set.of("over14", "terms", "privacy", "voice_ai");

        @Test
        @DisplayName("marketing_push가 포함되면 수신 동의와 처리 시각이 기록된다")
        void accept_whenMarketingPushIncluded_agreesMarketingPush() {
            // given
            final User user = seedUser();

            // when
            final AgreementResponse response = userAgreementService.accept(
                    user.getId(),
                    requestWith("2026-10-02", withMarketingPush())
            );

            // then
            assertThat(response.user().marketingPushAgreed()).isTrue();
            assertThat(response.user().marketingPushUpdatedAt()).isNotNull();
            final User reloaded = userRepository.findById(user.getId()).orElseThrow();
            assertThat(reloaded.getMarketingPushConsent().isAgreed()).isTrue();
        }

        @Test
        @DisplayName("marketing_push가 빠져도 약관 동의는 성공하고 미동의·처리 시각 null로 남는다")
        void accept_whenMarketingPushOmitted_succeedsWithoutConsent() {
            // given
            final User user = seedUser();

            // when
            final AgreementResponse response = userAgreementService.accept(
                    user.getId(),
                    requestWith("2026-10-02", REQUIRED_ITEMS)
            );

            // then
            assertThat(response.user().requiresOnboarding()).isFalse();
            assertThat(response.user().marketingPushAgreed()).isFalse();
            assertThat(response.user().marketingPushUpdatedAt()).isNull();
        }

        @Test
        @DisplayName("동의했던 유저가 재동의할 때 marketing_push가 빠지면 철회로 처리된다")
        void accept_whenReAgreeWithoutMarketingPush_withdrawsConsent() {
            // given
            final User user = seedUser();
            userAgreementService.accept(user.getId(), requestWith("2026-10-02", withMarketingPush()));

            // when
            final AgreementResponse response = userAgreementService.accept(
                    user.getId(),
                    requestWith("2026-11-01", REQUIRED_ITEMS)
            );

            // then
            assertThat(response.user().marketingPushAgreed()).isFalse();
            assertThat(response.user().marketingPushUpdatedAt()).isNotNull();
        }

        @Test
        @DisplayName("이미 동의한 유저가 다시 marketing_push를 포함해 재동의하면 처리 시각을 유지한다")
        void accept_whenReAgreeWithMarketingPush_keepsUpdatedAt() {
            // given
            final User user = seedUser();
            final LocalDateTime firstUpdatedAt = userAgreementService.accept(
                    user.getId(),
                    requestWith("2026-10-02", withMarketingPush())
            ).user().marketingPushUpdatedAt();

            // when
            final AgreementResponse response = userAgreementService.accept(
                    user.getId(),
                    requestWith("2026-11-01", withMarketingPush())
            );

            // then
            assertThat(response.user().marketingPushUpdatedAt()).isEqualTo(firstUpdatedAt);
        }

        private Set<String> withMarketingPush() {
            final Set<String> items = new HashSet<>(REQUIRED_ITEMS);
            items.add("marketing_push");
            return items;
        }

        private AgreementCreateRequest requestWith(final String termsVersion, final Set<String> items) {
            return new AgreementCreateRequest(termsVersion, items);
        }
    }
}
