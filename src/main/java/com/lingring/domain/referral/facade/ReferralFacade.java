package com.lingring.domain.referral.facade;

import com.lingring.domain.referral.domain.ReferralPeriod;
import com.lingring.domain.referral.dto.response.RedeemReferralResponse;
import com.lingring.domain.referral.dto.response.ReferralStatusResponse;
import com.lingring.domain.referral.exception.ReferralAlreadyRedeemedException;
import com.lingring.domain.referral.exception.ReferralPeriodExpiredException;
import com.lingring.domain.referral.exception.ReferralRejoinedUserException;
import com.lingring.domain.referral.exception.ReferralSelfNotAllowedException;
import com.lingring.domain.referral.service.ReferralService;
import com.lingring.domain.review.service.AnalysisQuotaService;
import com.lingring.domain.user.domain.User;
import com.lingring.domain.user.service.UserService;
import com.lingring.global.error.ErrorCode;
import com.lingring.global.error.exception.DomainException;
import com.lingring.global.error.exception.NotFoundException;
import com.lingring.global.util.DateTimeProvider;
import java.time.LocalDateTime;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class ReferralFacade {

    private static final int REWARD_PAID_TICKETS = 3;

    private final ReferralService referralService;
    private final UserService userService;
    private final AnalysisQuotaService analysisQuotaService;
    private final DateTimeProvider dateTimeProvider;

    @Transactional(readOnly = true)
    public ReferralStatusResponse getStatus(final Long userId) {
        final User user = userService.getById(userId);
        final ReferralPeriod period = ReferralPeriod.startingAt(user.getCreatedAt());
        final boolean redeemable = findIneligibility(user, period, dateTimeProvider.now()).isEmpty();
        return new ReferralStatusResponse(redeemable, period.getUntil());
    }

    @Transactional
    public RedeemReferralResponse redeem(final Long inviteeId, final String nickname) {
        final User invitee = userService.getById(inviteeId);
        final LocalDateTime now = dateTimeProvider.now();
        findIneligibility(invitee, ReferralPeriod.startingAt(invitee.getCreatedAt()), now)
                .ifPresent(exception -> {
                    throw exception;
                });
        final Long referrerId = findReferrerId(inviteeId, nickname);

        referralService.record(inviteeId, referrerId, now);
        analysisQuotaService.charge(inviteeId, REWARD_PAID_TICKETS);
        analysisQuotaService.charge(referrerId, REWARD_PAID_TICKETS);
        return new RedeemReferralResponse(analysisQuotaService.getStatus(inviteeId).paidTicket());
    }

    private Optional<DomainException> findIneligibility(
            final User user,
            final ReferralPeriod period,
            final LocalDateTime now
    ) {
        if (referralService.hasRedeemed(user.getId())) {
            return Optional.of(new ReferralAlreadyRedeemedException(user.getId()));
        }
        if (!period.isOpenAt(now)) {
            return Optional.of(new ReferralPeriodExpiredException(user.getId()));
        }
        if (userService.isRejoined(user)) {
            return Optional.of(new ReferralRejoinedUserException(user.getId()));
        }
        return Optional.empty();
    }

    private Long findReferrerId(final Long inviteeId, final String nickname) {
        final Long referrerId = userService.findIdByNickname(nickname)
                .orElseThrow(() -> new NotFoundException(
                        ErrorCode.REFERRER_NOT_FOUND,
                        "닉네임이 '%s'인 추천인을 찾을 수 없습니다.".formatted(nickname)
                ));
        if (referrerId.equals(inviteeId)) {
            throw new ReferralSelfNotAllowedException(inviteeId);
        }
        return referrerId;
    }
}
