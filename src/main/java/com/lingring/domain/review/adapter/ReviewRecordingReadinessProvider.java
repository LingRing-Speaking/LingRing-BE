package com.lingring.domain.review.adapter;

import com.lingring.domain.call.domain.port.RecordingReadinessProvider;
import com.lingring.domain.review.dao.CallRecordingRepository;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ReviewRecordingReadinessProvider implements RecordingReadinessProvider {

    private final CallRecordingRepository callRecordingRepository;

    @Override
    public Set<Long> findReadyCallIds(final List<Long> callIds) {
        if (callIds.isEmpty()) {
            return Set.of();
        }
        return Set.copyOf(callRecordingRepository.findCallIdsWithBothRecordings(callIds));
    }
}
