package com.lingring.domain.review.dao;

import com.lingring.domain.review.domain.recording.CallRecording;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CallRecordingRepository extends JpaRepository<CallRecording, Long> {

    Optional<CallRecording> findByCallIdAndUserId(Long callId, Long userId);
}
