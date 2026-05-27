package com.lingring.domain.user.facade;

import com.lingring.domain.auth.service.AppleAuthService;
import com.lingring.domain.auth.service.AuthService;
import com.lingring.domain.call.service.CallService;
import com.lingring.domain.user.domain.Provider;
import com.lingring.domain.user.domain.User;
import com.lingring.domain.user.domain.WithdrawReason;
import com.lingring.domain.user.domain.vo.AppleOAuthCredential;
import com.lingring.domain.user.event.UserWithdrawnEvent;
import com.lingring.domain.user.service.UserService;
import com.lingring.domain.user.service.UserStatsService;
import com.lingring.domain.moderation.service.UserBlockService;
import com.lingring.domain.expression.service.UserExpressionService;
import com.lingring.domain.moderation.service.UserReportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserWithdrawalFacade {

    private final UserService userService;
    private final UserStatsService userStatsService;
    private final UserBlockService userBlockService;
    private final UserExpressionService userExpressionService;
    private final UserReportService userReportService;
    private final CallService callService;
    private final AuthService authService;
    private final AppleAuthService appleAuthService;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public void withdraw(
            final Long userId,
            final WithdrawReason reason,
            final String description
    ) {
        final User user = userService.getById(userId);

        revokeAppleIfApplicable(user);
        callService.anonymizeUser(userId);
        userReportService.anonymizeReporter(userId);
        userBlockService.deleteByUserId(userId);
        userExpressionService.deleteByUserId(userId);
        userStatsService.deleteByUserId(userId);
        userService.delete(user);
        authService.logout(userId);

        eventPublisher.publishEvent(new UserWithdrawnEvent(userId, reason, description));
    }

    private void revokeAppleIfApplicable(final User user) {
        if (user.getProvider() != Provider.APPLE) {
            return;
        }
        final AppleOAuthCredential credential = user.getAppleCredential();
        if (credential == null) {
            log.info("Apple credential 없음, revoke 건너뜀. userId={}", user.getId());
            return;
        }
        appleAuthService.revoke(credential.getRefreshToken());
    }
}
