package com.lingring.domain.callanalysis.dao;

import com.lingring.domain.callanalysis.domain.CallRecording;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CallRecordingRepository extends JpaRepository<CallRecording, Long> {

    Optional<CallRecording> findByCallIdAndUserId(Long callId, Long userId);
}
