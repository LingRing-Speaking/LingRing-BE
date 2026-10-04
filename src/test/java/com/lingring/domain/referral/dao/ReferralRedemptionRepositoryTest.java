package com.lingring.domain.referral.dao;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import com.lingring.domain.referral.domain.ReferralRedemption;
import com.lingring.global.config.RepositoryTestHelper;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class ReferralRedemptionRepositoryTest extends RepositoryTestHelper {

    private static final LocalDateTime REDEEMED_AT = LocalDateTime.of(2026, 10, 3, 14, 0);

    @Autowired
    private ReferralRedemptionRepository referralRedemptionRepository;

    @Nested
    @DisplayName("deleteByInviteeId")
    class DeleteByInviteeId {

        @Test
        @DisplayName("입력자의 기록만 삭제하고 삭제 건수를 반환한다")
        void deleteByInviteeId_deletesOnlyInviteeRow() {
            // given
            referralRedemptionRepository.save(ReferralRedemption.record(10L, 1L, REDEEMED_AT));
            referralRedemptionRepository.save(ReferralRedemption.record(20L, 1L, REDEEMED_AT));

            // when
            final int deleted = referralRedemptionRepository.deleteByInviteeId(10L);

            // then
            assertThat(deleted).isEqualTo(1);
            assertThat(referralRedemptionRepository.existsByInviteeId(10L)).isFalse();
            assertThat(referralRedemptionRepository.existsByInviteeId(20L)).isTrue();
        }
    }

    @Nested
    @DisplayName("anonymizeReferrer")
    class AnonymizeReferrer {

        @Test
        @DisplayName("해당 추천인의 referrerId만 NULL로 바꾸고 행은 유지한다")
        void anonymizeReferrer_nullsOnlyMatchingReferrer() {
            // given
            referralRedemptionRepository.save(ReferralRedemption.record(10L, 1L, REDEEMED_AT));
            referralRedemptionRepository.save(ReferralRedemption.record(20L, 2L, REDEEMED_AT));

            // when
            final int anonymized = referralRedemptionRepository.anonymizeReferrer(1L);

            // then
            assertThat(anonymized).isEqualTo(1);
            assertThat(referralRedemptionRepository.findAll())
                    .extracting(ReferralRedemption::getInviteeId, ReferralRedemption::getReferrerId)
                    .containsExactlyInAnyOrder(
                            tuple(10L, null),
                            tuple(20L, 2L)
                    );
        }
    }
}
