package com.lingring.domain.review.dao;

import com.lingring.domain.review.domain.quota.AnalysisQuota;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AnalysisQuotaRepository extends JpaRepository<AnalysisQuota, Long> {

    Optional<AnalysisQuota> findByUserId(Long userId);
}
