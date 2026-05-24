package com.lingring.domain.callanalysis.dao;

import com.lingring.domain.callanalysis.dao.dto.CallAnalysisIdProjection;
import com.lingring.domain.callanalysis.domain.CallAnalysis;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CallAnalysisRepository extends JpaRepository<CallAnalysis, Long> {

    Optional<CallAnalysis> findByCallIdAndUserId(Long callId, Long userId);

    boolean existsByCallIdAndUserId(Long callId, Long userId);

    @Query("""
            SELECT new com.lingring.domain.callanalysis.dao.dto.CallAnalysisIdProjection(ca.callId, ca.id)
            FROM CallAnalysis ca
            WHERE ca.userId = :userId
              AND ca.callId IN :callIds
              AND ca.requested = true
            """)
    List<CallAnalysisIdProjection> findRequestedAnalysisIds(
            @Param("userId") Long userId,
            @Param("callIds") Collection<Long> callIds
    );
}
