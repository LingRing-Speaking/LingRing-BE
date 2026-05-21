package com.lingring.domain.call.dao;

import com.lingring.domain.call.domain.CallTranscript;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CallTranscriptRepository extends JpaRepository<CallTranscript, Long> {

    Optional<CallTranscript> findByCallId(Long callId);
}
