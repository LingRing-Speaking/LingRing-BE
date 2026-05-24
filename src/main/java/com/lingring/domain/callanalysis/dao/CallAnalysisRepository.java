package com.lingring.domain.callanalysis.dao;

import com.lingring.domain.callanalysis.domain.CallAnalysis;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CallAnalysisRepository extends JpaRepository<CallAnalysis, Long> {

    Optional<CallAnalysis> findByCallIdAndUserId(Long callId, Long userId);

    boolean existsByCallIdAndUserId(Long callId, Long userId);
}
