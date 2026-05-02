package com.lingring.domain.call.dao;

import com.lingring.domain.call.dao.dto.CallSummaryProjection;
import com.lingring.domain.call.domain.Call;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CallRepository extends JpaRepository<Call, Long> {

    Optional<Call> findByRoomId(UUID roomId);

    @Query("""
            SELECT c.id AS id,
                   u.id AS partnerId,
                   u.name.value AS partnerName,
                   c.startedAt AS startedAt,
                   c.endedAt AS endedAt
            FROM Call c, User u
            WHERE c.endedAt IS NOT NULL
              AND ((c.userAId = :userId AND u.id = c.userBId)
                   OR (c.userBId = :userId AND u.id = c.userAId))
            ORDER BY c.startedAt DESC
            """)
    Slice<CallSummaryProjection> findEndedSummariesByUserId(
            @Param("userId") Long userId,
            Pageable pageable
    );
}
