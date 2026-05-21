package com.lingring.domain.call.service;

import com.lingring.domain.call.dao.CallRecordingRepository;
import com.lingring.domain.call.dao.CallRepository;
import com.lingring.domain.call.dao.CallTranscriptRepository;
import com.lingring.domain.call.domain.Call;
import com.lingring.domain.call.domain.CallRecording;
import com.lingring.domain.call.domain.CallTranscript;
import com.lingring.domain.call.domain.vo.RecordingReference;
import com.lingring.domain.call.domain.vo.TranscriptContent;
import com.lingring.domain.call.domain.vo.TranscriptSegment;
import com.lingring.domain.call.dto.response.CallTranscriptResponse;
import com.lingring.domain.call.dto.response.CallTranscriptStartResponse;
import com.lingring.domain.call.event.CallTranscriptRequestedEvent;
import com.lingring.domain.call.exception.CallActiveException;
import com.lingring.domain.call.exception.CallNotFoundException;
import com.lingring.domain.call.exception.CallParticipantMismatchException;
import com.lingring.domain.call.exception.CallRecordingsNotReadyException;
import com.lingring.domain.call.exception.CallTranscriptNotFoundException;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CallTranscriptService {

    private final CallRepository callRepository;
    private final CallRecordingRepository callRecordingRepository;
    private final CallTranscriptRepository callTranscriptRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public CallTranscriptStartResponse requestAnalysis(final Long callId, final Long userId) {
        final Call call = requireParticipantCall(callId, userId);
        if (call.isActive()) {
            throw new CallActiveException(callId);
        }

        final Optional<CallTranscript> existing = callTranscriptRepository.findByCallId(callId);
        if (existing.isPresent()) {
            return CallTranscriptStartResponse.from(existing.get());
        }

        final List<RecordingReference> references = collectRecordings(call);

        final CallTranscript transcript = callTranscriptRepository.save(CallTranscript.startProcessing(callId));
        eventPublisher.publishEvent(new CallTranscriptRequestedEvent(callId, references));

        return CallTranscriptStartResponse.from(transcript);
    }

    @Transactional(readOnly = true)
    public CallTranscriptResponse getTranscript(final Long callId, final Long userId) {
        requireParticipantCall(callId, userId);
        final CallTranscript transcript = callTranscriptRepository.findByCallId(callId)
                .orElseThrow(() -> new CallTranscriptNotFoundException(callId));
        return CallTranscriptResponse.from(transcript);
    }

    @Transactional
    public void complete(final Long callId, final List<TranscriptSegment> segments) {
        final CallTranscript transcript = callTranscriptRepository.findByCallId(callId)
                .orElseThrow(() -> new CallTranscriptNotFoundException(callId));
        transcript.complete(new TranscriptContent(segments));
    }

    private Call requireParticipantCall(final Long callId, final Long userId) {
        final Call call = callRepository.findById(callId)
                .orElseThrow(() -> new CallNotFoundException(callId));
        if (!call.involves(userId)) {
            throw new CallParticipantMismatchException(callId, userId);
        }
        return call;
    }

    private List<RecordingReference> collectRecordings(final Call call) {
        final CallRecording recordingA = callRecordingRepository
                .findByCallIdAndUserId(call.getId(), call.getUserAId())
                .orElseThrow(() -> new CallRecordingsNotReadyException(call.getId()));
        final CallRecording recordingB = callRecordingRepository
                .findByCallIdAndUserId(call.getId(), call.getUserBId())
                .orElseThrow(() -> new CallRecordingsNotReadyException(call.getId()));
        return List.of(
                new RecordingReference(recordingA.getUserId(), recordingA.getRecordingKey()),
                new RecordingReference(recordingB.getUserId(), recordingB.getRecordingKey())
        );
    }
}
