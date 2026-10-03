package com.lingring.domain.referral.facade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.lingring.domain.referral.dao.ReferralRedemptionRepository;
import com.lingring.domain.referral.domain.ReferralRedemption;
import com.lingring.domain.referral.dto.response.RedeemReferralResponse;
import com.lingring.domain.referral.dto.response.ReferralStatusResponse;
import com.lingring.domain.referral.exception.ReferralAlreadyRedeemedException;
import com.lingring.domain.referral.exception.ReferralPeriodExpiredException;
import com.lingring.domain.referral.exception.ReferralRejoinedUserException;
import com.lingring.domain.referral.exception.ReferralSelfNotAllowedException;
import com.lingring.domain.review.dao.AnalysisQuotaRepository;
import com.lingring.domain.review.domain.quota.AnalysisQuota;
import com.lingring.domain.user.dao.UserRepository;
import com.lingring.domain.user.dao.WithdrawnIdentityRepository;
import com.lingring.domain.user.domain.Provider;
import com.lingring.domain.user.domain.User;
import com.lingring.domain.user.domain.WithdrawnIdentity;
import com.lingring.domain.user.domain.service.SocialIdentityHasher;
import com.lingring.domain.user.domain.vo.Name;
import com.lingring.global.config.ServiceIntegrationHelper;
import com.lingring.global.error.ErrorCode;
import com.lingring.global.error.exception.NotFoundException;
import com.lingring.global.util.DateTimeProvider;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

class ReferralFacadeTest extends ServiceIntegrationHelper {

    @Autowired
    private ReferralFacade referralFacade;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ReferralRedemptionRepository referralRedemptionRepository;

    @Autowired
    private AnalysisQuotaRepository analysisQuotaRepository;

    @Autowired
    private WithdrawnIdentityRepository withdrawnIdentityRepository;

    @Autowired
    private SocialIdentityHasher socialIdentityHasher;

    @Autowired
    private DateTimeProvider dateTimeProvider;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Nested
    @DisplayName("redeem: 추천인 닉네임 입력")
    class Redeem {

        @Test
        @DisplayName("입력자와 추천인에게 황금티켓을 3장씩 지급하고 입력 기록을 남긴다")
        void redeem_whenValid_chargesBothAndRecords() {
            // given
            final User referrer = saveUser("추천인", "referrer-sub");
            final User invitee = saveUser("신규", "invitee-sub");

            // when
            final RedeemReferralResponse response = referralFacade.redeem(invitee.getId(), "추천인");

            // then
            assertThat(response.paidTicket()).isEqualTo(3);
            assertThat(paidTicketOf(invitee)).isEqualTo(3);
            assertThat(paidTicketOf(referrer)).isEqualTo(3);
            assertThat(referralRedemptionRepository.findAll())
                    .singleElement()
                    .extracting(ReferralRedemption::getInviteeId, ReferralRedemption::getReferrerId)
                    .containsExactly(invitee.getId(), referrer.getId());
        }

        @Test
        @DisplayName("이미 황금티켓이 있는 추천인에게는 기존 티켓에 더해 지급한다")
        void redeem_whenReferrerHasTickets_addsToExisting() {
            // given
            final User referrer = saveUser("추천인", "referrer-sub");
            final User invitee = saveUser("신규", "invitee-sub");
            final AnalysisQuota quota = AnalysisQuota.initial(referrer.getId());
            quota.charge(4);
            analysisQuotaRepository.save(quota);

            // when
            referralFacade.redeem(invitee.getId(), "추천인");

            // then
            assertThat(paidTicketOf(referrer)).isEqualTo(7);
        }

        @Test
        @DisplayName("닉네임은 대소문자를 구분하지 않고 일치한다")
        void redeem_whenNicknameCaseDiffers_matches() {
            // given
            final User referrer = saveUser("LingRing", "referrer-sub");
            final User invitee = saveUser("신규", "invitee-sub");

            // when
            referralFacade.redeem(invitee.getId(), "lingring");

            // then
            assertThat(paidTicketOf(referrer)).isEqualTo(3);
        }

        @Test
        @DisplayName("이미 입력했으면 REFERRAL_ALREADY_REDEEMED 예외가 발생한다")
        void redeem_whenAlreadyRedeemed_throwsAlreadyRedeemed() {
            // given
            saveUser("추천인", "referrer-sub");
            final User invitee = saveUser("신규", "invitee-sub");
            referralFacade.redeem(invitee.getId(), "추천인");

            // when & then
            assertThatThrownBy(() -> referralFacade.redeem(invitee.getId(), "추천인"))
                    .isInstanceOf(ReferralAlreadyRedeemedException.class);
            assertThat(paidTicketOf(invitee)).isEqualTo(3);
        }

        @Test
        @DisplayName("가입 후 7일이 지났으면 REFERRAL_PERIOD_EXPIRED 예외가 발생하고 지급하지 않는다")
        void redeem_whenPeriodExpired_throwsPeriodExpired() {
            // given
            final User referrer = saveUser("추천인", "referrer-sub");
            final User invitee = saveUser("신규", "invitee-sub");
            setJoinedAt(invitee, dateTimeProvider.now().minusDays(7).minusMinutes(1));

            // when & then
            assertThatThrownBy(() -> referralFacade.redeem(invitee.getId(), "추천인"))
                    .isInstanceOf(ReferralPeriodExpiredException.class);
            assertThat(analysisQuotaRepository.findByUserId(referrer.getId())).isEmpty();
        }

        @Test
        @DisplayName("가입 후 7일 이내면 입력할 수 있다")
        void redeem_whenWithinPeriod_succeeds() {
            // given
            saveUser("추천인", "referrer-sub");
            final User invitee = saveUser("신규", "invitee-sub");
            setJoinedAt(invitee, dateTimeProvider.now().minusDays(7).plusMinutes(1));

            // when
            final RedeemReferralResponse response = referralFacade.redeem(invitee.getId(), "추천인");

            // then
            assertThat(response.paidTicket()).isEqualTo(3);
        }

        @Test
        @DisplayName("탈퇴 이력이 있는 재가입자면 REFERRAL_NOT_ELIGIBLE_REJOINED 예외가 발생한다")
        void redeem_whenRejoined_throwsRejoinedUser() {
            // given
            saveUser("추천인", "referrer-sub");
            final User invitee = saveUser("신규", "invitee-sub");
            recordWithdrawn("invitee-sub");

            // when & then
            assertThatThrownBy(() -> referralFacade.redeem(invitee.getId(), "추천인"))
                    .isInstanceOf(ReferralRejoinedUserException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.REFERRAL_NOT_ELIGIBLE_REJOINED);
        }

        @Test
        @DisplayName("해당 닉네임의 사용자가 없으면 REFERRER_NOT_FOUND 예외가 발생한다")
        void redeem_whenNicknameNotFound_throwsReferrerNotFound() {
            // given
            final User invitee = saveUser("신규", "invitee-sub");

            // when & then
            assertThatThrownBy(() -> referralFacade.redeem(invitee.getId(), "없는닉네임"))
                    .isInstanceOf(NotFoundException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.REFERRER_NOT_FOUND);
        }

        @Test
        @DisplayName("닉네임 형식에 맞지 않는 입력도 REFERRER_NOT_FOUND로 응답한다")
        void redeem_whenNicknameMalformed_throwsReferrerNotFound() {
            // given
            final User invitee = saveUser("신규", "invitee-sub");

            // when & then
            assertThatThrownBy(() -> referralFacade.redeem(invitee.getId(), "!@#"))
                    .isInstanceOf(NotFoundException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.REFERRER_NOT_FOUND);
        }

        @Test
        @DisplayName("자기 닉네임을 입력하면 REFERRAL_SELF_NOT_ALLOWED 예외가 발생하고 지급하지 않는다")
        void redeem_whenSelfNickname_throwsSelfNotAllowed() {
            // given
            final User invitee = saveUser("신규", "invitee-sub");

            // when & then
            assertThatThrownBy(() -> referralFacade.redeem(invitee.getId(), "신규"))
                    .isInstanceOf(ReferralSelfNotAllowedException.class);
            assertThat(analysisQuotaRepository.findByUserId(invitee.getId())).isEmpty();
            assertThat(referralRedemptionRepository.findAll()).isEmpty();
        }

        @Test
        @DisplayName("기간이 지났으면 닉네임이 없어도 기간 만료를 먼저 응답한다")
        void redeem_whenExpiredAndNicknameMissing_reportsExpiredFirst() {
            // given
            final User invitee = saveUser("신규", "invitee-sub");
            setJoinedAt(invitee, dateTimeProvider.now().minusDays(8));

            // when & then
            assertThatThrownBy(() -> referralFacade.redeem(invitee.getId(), "없는닉네임"))
                    .isInstanceOf(ReferralPeriodExpiredException.class);
        }
    }

    @Nested
    @DisplayName("getStatus: 추천인 입력 가능 여부")
    class GetStatus {

        @Test
        @DisplayName("미입력·기간 내·재가입자 아님이면 입력 가능하고 마감은 가입 시각 + 7일이다")
        void getStatus_whenEligible_returnsRedeemable() {
            // given
            final User invitee = saveUser("신규", "invitee-sub");
            final LocalDateTime joinedAt = dateTimeProvider.now().minusDays(1);
            setJoinedAt(invitee, joinedAt);

            // when
            final ReferralStatusResponse response = referralFacade.getStatus(invitee.getId());

            // then
            assertThat(response.redeemable()).isTrue();
            assertThat(response.redeemableUntil()).isEqualTo(joinedAt.plusDays(7));
        }

        @Test
        @DisplayName("이미 입력했으면 입력 불가다")
        void getStatus_whenRedeemed_returnsNotRedeemable() {
            // given
            saveUser("추천인", "referrer-sub");
            final User invitee = saveUser("신규", "invitee-sub");
            referralFacade.redeem(invitee.getId(), "추천인");

            // when & then
            assertThat(referralFacade.getStatus(invitee.getId()).redeemable()).isFalse();
        }

        @Test
        @DisplayName("기간이 지났으면 입력 불가지만 마감 시각은 반환한다")
        void getStatus_whenExpired_returnsNotRedeemableWithUntil() {
            // given
            final User invitee = saveUser("신규", "invitee-sub");
            final LocalDateTime joinedAt = dateTimeProvider.now().minusDays(8);
            setJoinedAt(invitee, joinedAt);

            // when
            final ReferralStatusResponse response = referralFacade.getStatus(invitee.getId());

            // then
            assertThat(response.redeemable()).isFalse();
            assertThat(response.redeemableUntil()).isEqualTo(joinedAt.plusDays(7));
        }

        @Test
        @DisplayName("재가입자면 입력 불가다")
        void getStatus_whenRejoined_returnsNotRedeemable() {
            // given
            final User invitee = saveUser("신규", "invitee-sub");
            recordWithdrawn("invitee-sub");

            // when & then
            assertThat(referralFacade.getStatus(invitee.getId()).redeemable()).isFalse();
        }
    }

    private User saveUser(final String nickname, final String providerSub) {
        return userRepository.save(
                User.createFromOAuth(Provider.KAKAO, providerSub, new Name(nickname), null)
        );
    }

    private void setJoinedAt(final User user, final LocalDateTime joinedAt) {
        jdbcTemplate.update(
                "UPDATE users SET created_at = ? WHERE id = ?",
                Timestamp.valueOf(joinedAt), user.getId()
        );
    }

    private void recordWithdrawn(final String providerSub) {
        withdrawnIdentityRepository.save(WithdrawnIdentity.record(
                socialIdentityHasher.hash(Provider.KAKAO, providerSub),
                dateTimeProvider.now().minusMonths(1)
        ));
    }

    private int paidTicketOf(final User user) {
        return analysisQuotaRepository.findByUserId(user.getId()).orElseThrow().paidRemaining();
    }
}
