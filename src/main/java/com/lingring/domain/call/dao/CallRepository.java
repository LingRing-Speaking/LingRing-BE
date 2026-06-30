package com.lingring.domain.call.dao;

import com.lingring.domain.call.dao.dto.CallSummaryProjection;
import com.lingring.domain.call.domain.Call;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CallRepository extends JpaRepository<Call, Long> {

    Optional<Call> findByRoomId(UUID roomId);

    @Query("""
            SELECT c.id AS id,
                   u.id AS partnerId,
                   u.name.value AS partnerName,
                   u.profileImage.value AS partnerProfileImage,
                   c.startedAt AS startedAt,
                   c.endedAt AS endedAt,
                   c.durationSec AS durationSec
            FROM Call c
            LEFT JOIN User u ON (u.id = c.userBId AND c.userAId = :userId)
                             OR (u.id = c.userAId AND c.userBId = :userId)
            WHERE c.endedAt IS NOT NULL
              AND (c.userAId = :userId OR c.userBId = :userId)
            ORDER BY c.startedAt DESC
            """)
    Slice<CallSummaryProjection> findEndedSummariesByUserId(
            @Param("userId") Long userId,
            Pageable pageable
    );

    @Modifying(clearAutomatically = true)
    @Query("""
            UPDATE Call c
               SET c.userAId = CASE WHEN c.userAId = :userId THEN null ELSE c.userAId END,
                   c.userBId = CASE WHEN c.userBId = :userId THEN null ELSE c.userBId END
             WHERE c.userAId = :userId OR c.userBId = :userId
            """)
    int anonymizeUser(@Param("userId") Long userId);
}
