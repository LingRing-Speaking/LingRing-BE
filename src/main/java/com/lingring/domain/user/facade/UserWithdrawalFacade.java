package com.lingring.domain.user.facade;

import com.lingring.domain.auth.service.AuthService;
import com.lingring.domain.call.service.CallService;
import com.lingring.domain.user.domain.User;
import com.lingring.domain.user.service.UserService;
import com.lingring.domain.user.service.UserStatsService;
import com.lingring.domain.userblock.service.UserBlockService;
import com.lingring.domain.userexpression.service.UserExpressionService;
import com.lingring.domain.userreport.service.UserReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

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

    @Transactional
    public void withdraw(final Long userId) {
        final User user = userService.getById(userId);

        callService.anonymizeUser(userId);
        userReportService.anonymizeReporter(userId);
        userBlockService.deleteByUserId(userId);
        userExpressionService.deleteByUserId(userId);
        userStatsService.deleteByUserId(userId);
        userService.delete(user);
        authService.logout(userId);
    }
}
