package com.lingring.domain.review.dao;

import com.lingring.domain.review.domain.transcript.CallTranscript;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CallTranscriptRepository extends JpaRepository<CallTranscript, Long> {

    Optional<CallTranscript> findByCallId(Long callId);
}
