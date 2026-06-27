package com.lingring.domain.review.dao;

import com.lingring.domain.review.domain.recording.CallRecording;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CallRecordingRepository extends JpaRepository<CallRecording, Long> {

    Optional<CallRecording> findByCallIdAndUserId(Long callId, Long userId);

    @Query("""
            SELECT cr.callId
            FROM CallRecording cr
            WHERE cr.callId IN :callIds
            GROUP BY cr.callId
            HAVING COUNT(cr) = 2
            """)
    List<Long> findCallIdsWithBothRecordings(@Param("callIds") Collection<Long> callIds);
}
