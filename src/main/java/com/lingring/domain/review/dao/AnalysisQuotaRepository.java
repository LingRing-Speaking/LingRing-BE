package com.lingring.domain.review.dao;

import com.lingring.domain.review.domain.quota.AnalysisQuota;
import jakarta.persistence.LockModeType;
import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AnalysisQuotaRepository extends JpaRepository<AnalysisQuota, Long> {

    Optional<AnalysisQuota> findByUserId(Long userId);

    // 엔티티 write-back이 paid_ticket 전체를 덮어써 addPaidTicket 지급을 지우지 않도록 행을 잠근다
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT q FROM AnalysisQuota q WHERE q.userId = :userId")
    Optional<AnalysisQuota> findByUserIdForUpdate(@Param("userId") Long userId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            UPDATE AnalysisQuota q
               SET q.paidTicket.count = q.paidTicket.count + :amount,
                   q.updatedAt = :now
             WHERE q.userId = :userId
            """)
    int addPaidTicket(
            @Param("userId") Long userId,
            @Param("amount") int amount,
            @Param("now") LocalDateTime now
    );
}
