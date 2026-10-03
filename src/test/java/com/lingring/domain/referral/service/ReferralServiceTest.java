package com.lingring.domain.referral.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.lingring.domain.referral.dao.ReferralRedemptionRepository;
import com.lingring.domain.referral.domain.ReferralRedemption;
import com.lingring.domain.referral.exception.ReferralAlreadyRedeemedException;
import com.lingring.global.config.ServiceIntegrationHelper;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class ReferralServiceTest extends ServiceIntegrationHelper {

    private static final LocalDateTime REDEEMED_AT = LocalDateTime.of(2026, 10, 3, 14, 0);

    @Autowired
    private ReferralService referralService;

    @Autowired
    private ReferralRedemptionRepository referralRedemptionRepository;

    @Nested
    @DisplayName("record: 추천인 입력 기록")
    class Record {

        @Test
        @DisplayName("입력 기록을 저장한다")
        void record_savesRedemption() {
            // when
            referralService.record(10L, 1L, REDEEMED_AT);

            // then
            assertThat(referralService.hasRedeemed(10L)).isTrue();
        }

        @Test
        @DisplayName("같은 입력자의 기록이 이미 있으면 unique 위반을 REFERRAL_ALREADY_REDEEMED로 바꿔 던진다")
        void record_whenInviteeAlreadyRecorded_throwsAlreadyRedeemed() {
            // given
            referralRedemptionRepository.save(ReferralRedemption.record(10L, 1L, REDEEMED_AT));

            // when & then
            assertThatThrownBy(() -> referralService.record(10L, 2L, REDEEMED_AT))
                    .isInstanceOf(ReferralAlreadyRedeemedException.class);
            assertThat(referralRedemptionRepository.findAll())
                    .singleElement()
                    .extracting(ReferralRedemption::getReferrerId)
                    .isEqualTo(1L);
        }
    }
}
