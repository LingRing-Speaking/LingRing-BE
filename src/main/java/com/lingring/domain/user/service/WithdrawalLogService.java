package com.lingring.domain.user.service;

import com.lingring.domain.user.dao.WithdrawalLogRepository;
import com.lingring.domain.user.domain.WithdrawReason;
import com.lingring.domain.user.domain.WithdrawalLog;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class WithdrawalLogService {

    private final WithdrawalLogRepository withdrawalLogRepository;

    @Transactional
    public void record(final WithdrawReason reason, final String description) {
        withdrawalLogRepository.save(WithdrawalLog.record(reason, description));
    }
}
