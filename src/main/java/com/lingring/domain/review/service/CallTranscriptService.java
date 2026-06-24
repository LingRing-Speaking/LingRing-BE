package com.lingring.domain.review.service;

import com.lingring.domain.review.dao.CallRecordingRepository;
import com.lingring.domain.call.dao.CallRepository;
import com.lingring.domain.review.dao.CallTranscriptRepository;
import com.lingring.domain.call.domain.Call;
import com.lingring.domain.review.domain.policy.CallAnalysisPolicy;
import com.lingring.domain.review.domain.recording.CallRecording;
import com.lingring.domain.review.domain.transcript.CallTranscript;
import com.lingring.domain.review.domain.recording.vo.RecordingReference;
import com.lingring.domain.review.domain.transcript.vo.TranscriptContent;
import com.lingring.domain.review.domain.transcript.vo.TranscriptSegment;
import com.lingring.domain.review.dto.response.CallTranscriptResponse;
import com.lingring.domain.call.exception.CallActiveException;
import com.lingring.domain.call.exception.CallNotFoundException;
import com.lingring.domain.call.exception.CallParticipantMismatchException;
import com.lingring.domain.review.exception.CallRecordingsNotReadyException;
import com.lingring.domain.review.exception.CallTranscriptNotFoundException;
import com.lingring.domain.review.exception.CallTranscriptNotReadyException;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CallTranscriptService {

    private final CallRepository callRepository;
    private final CallRecordingRepository callRecordingRepository;
    private final CallTranscriptRepository callTranscriptRepository;
    private final CallAnalysisPolicy callAnalysisPolicy;

    @Transactional
    public StartTranscriptResult startTranscript(final Long callId, final Long userId) {
        final Call call = requireParticipantCall(callId, userId);
        if (call.isActive()) {
            throw new CallActiveException(callId);
        }
        callAnalysisPolicy.requireEnoughToAnalyze(callId, call.getDurationSec());

        final Optional<CallTranscript> existing = callTranscriptRepository.findByCallId(callId);
        if (existing.isPresent()) {
            return StartTranscriptResult.existing(existing.get(), call.getUserAId(), call.getUserBId());
        }

        final List<RecordingReference> references = collectRecordings(call);
        final CallTranscript transcript = callTranscriptRepository.save(CallTranscript.create(callId));
        return StartTranscriptResult.created(transcript, call.getUserAId(), call.getUserBId(), references);
    }

    @Transactional(readOnly = true)
    public CallTranscriptResponse getTranscript(final Long callId, final Long userId) {
        requireParticipantCall(callId, userId);
        final CallTranscript transcript = getTranscript(callId);
        if (!transcript.isCompleted()) {
            throw new CallTranscriptNotReadyException(callId);
        }
        return CallTranscriptResponse.from(transcript);
    }

    @Transactional
    public void complete(final Long callId, final List<TranscriptSegment> segments) {
        final CallTranscript transcript = getTranscript(callId);
        transcript.complete(new TranscriptContent(segments));
    }

    private CallTranscript getTranscript(final Long callId) {
        return callTranscriptRepository.findByCallId(callId)
                .orElseThrow(() -> new CallTranscriptNotFoundException(callId));
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

    public record StartTranscriptResult(
            CallTranscript transcript,
            Long userAId,
            Long userBId,
            List<RecordingReference> recordings,
            boolean freshlyCreated
    ) {

        public static StartTranscriptResult created(
                final CallTranscript transcript,
                final Long userAId,
                final Long userBId,
                final List<RecordingReference> recordings
        ) {
            return new StartTranscriptResult(transcript, userAId, userBId, recordings, true);
        }

        public static StartTranscriptResult existing(
                final CallTranscript transcript,
                final Long userAId,
                final Long userBId
        ) {
            return new StartTranscriptResult(transcript, userAId, userBId, List.of(), false);
        }
    }
}
