package com.lingring.domain.user.service;

import com.lingring.domain.user.dao.WithdrawnIdentityRepository;
import com.lingring.domain.user.domain.WithdrawnIdentity;
import com.lingring.global.util.DateTimeProvider;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class WithdrawnIdentityPurgeService {

    private final WithdrawnIdentityRepository withdrawnIdentityRepository;
    private final DateTimeProvider dateTimeProvider;

    @Transactional
    public int purgeExpired() {
        final LocalDateTime threshold = WithdrawnIdentity.expirationThreshold(dateTimeProvider.now());
        final int purged = withdrawnIdentityRepository.deleteAllWithdrawnBefore(threshold);
        log.info("보관 기간이 지난 탈퇴 계정 해시 파기 완료. threshold={}, purged={}", threshold, purged);
        return purged;
    }
}
