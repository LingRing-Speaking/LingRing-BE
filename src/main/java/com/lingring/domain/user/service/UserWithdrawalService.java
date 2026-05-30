package com.lingring.domain.user.service;

import com.lingring.domain.user.dao.UserRepository;
import com.lingring.domain.user.domain.User;
import com.lingring.domain.user.domain.WithdrawReason;
import com.lingring.domain.user.domain.port.OAuthCredentialRevoker;
import com.lingring.domain.user.domain.vo.AppleOAuthCredential;
import com.lingring.domain.user.event.UserWithdrawnEvent;
import com.lingring.global.error.ErrorCode;
import com.lingring.global.error.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserWithdrawalService {

    private final UserRepository userRepository;
    private final OAuthCredentialRevoker oauthCredentialRevoker;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public void withdraw(
            final Long userId,
            final WithdrawReason reason,
            final String description
    ) {
        final User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException(
                        ErrorCode.USER_NOT_FOUND,
                        "ID가 %d인 사용자를 찾을 수 없습니다.".formatted(userId)
                ));
        revokeOAuthCredentialIfApplicable(user);
        eventPublisher.publishEvent(new UserWithdrawnEvent(userId, reason, description));
        userRepository.delete(user);
    }

    private void revokeOAuthCredentialIfApplicable(final User user) {
        final AppleOAuthCredential credential = user.getAppleCredential();
        if (credential == null) {
            log.info("OAuth credential 없음, revoke 건너뜀. userId={}", user.getId());
            return;
        }
        oauthCredentialRevoker.revoke(user.getProvider(), credential.getRefreshToken());
    }
}
