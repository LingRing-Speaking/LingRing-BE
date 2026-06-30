package com.lingring.domain.review.service;

import com.lingring.domain.review.dao.AnalysisQuotaRepository;
import com.lingring.domain.review.domain.quota.AnalysisQuota;
import com.lingring.domain.review.dto.response.AnalysisQuotaResponse;
import com.lingring.global.util.DateTimeProvider;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AnalysisQuotaService {

    private final AnalysisQuotaRepository analysisQuotaRepository;
    private final DateTimeProvider dateTimeProvider;

    @Transactional
    public void consumeFreeDaily(final Long userId) {
        final LocalDate today = dateTimeProvider.now().toLocalDate();
        final AnalysisQuota quota = analysisQuotaRepository.findByUserId(userId)
                .orElseGet(() -> analysisQuotaRepository.save(AnalysisQuota.initial(userId)));
        quota.consumeOn(today);
    }

    @Transactional(readOnly = true)
    public AnalysisQuotaResponse getStatus(final Long userId) {
        final LocalDate today = dateTimeProvider.now().toLocalDate();
        final AnalysisQuota quota = analysisQuotaRepository.findByUserId(userId)
                .orElseGet(() -> AnalysisQuota.initial(userId));
        final LocalDateTime nextResetAt = today.plusDays(1).atStartOfDay();
        return new AnalysisQuotaResponse(quota.freeRemainingOn(today), quota.paidRemaining(), nextResetAt);
    }
}
