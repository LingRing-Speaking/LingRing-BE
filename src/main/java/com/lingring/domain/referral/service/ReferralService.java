package com.lingring.domain.referral.service;

import com.lingring.domain.referral.dao.ReferralRedemptionRepository;
import com.lingring.domain.referral.domain.ReferralRedemption;
import com.lingring.domain.referral.exception.ReferralAlreadyRedeemedException;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ReferralService {

    private final ReferralRedemptionRepository referralRedemptionRepository;

    @Transactional(readOnly = true)
    public boolean hasRedeemed(final Long inviteeId) {
        return referralRedemptionRepository.existsByInviteeId(inviteeId);
    }

    @Transactional
    public void record(final Long inviteeId, final Long referrerId, final LocalDateTime redeemedAt) {
        try {
            referralRedemptionRepository.saveAndFlush(ReferralRedemption.record(inviteeId, referrerId, redeemedAt));
        } catch (final DataIntegrityViolationException e) {
            throw new ReferralAlreadyRedeemedException(inviteeId);
        }
    }
}
