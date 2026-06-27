package com.lingring.domain.review.adapter;

import static org.assertj.core.api.Assertions.assertThat;

import com.lingring.domain.call.dao.CallRepository;
import com.lingring.domain.call.domain.Call;
import com.lingring.domain.review.dao.CallRecordingRepository;
import com.lingring.domain.review.domain.recording.CallRecording;
import com.lingring.global.config.ServiceIntegrationHelper;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class ReviewRecordingReadinessProviderTest extends ServiceIntegrationHelper {

    private static final LocalDateTime ENDED_AT = LocalDateTime.of(2026, 5, 20, 10, 0);
    private static final LocalDateTime STARTED_AT = ENDED_AT.minusMinutes(5);

    @Autowired
    private ReviewRecordingReadinessProvider provider;

    @Autowired
    private CallRepository callRepository;

    @Autowired
    private CallRecordingRepository callRecordingRepository;

    @Test
    @DisplayName("두 화자 녹음이 모두 있는 통화만 ready 로 반환한다")
    void findReadyCallIds_returnsOnlyCallsWithBothRecordings() {
        // given
        final Call both = saveEndedCall(1L, 2L);
        final Call one = saveEndedCall(3L, 4L);
        final Call none = saveEndedCall(5L, 6L);
        saveRecording(both.getId(), 1L);
        saveRecording(both.getId(), 2L);
        saveRecording(one.getId(), 3L);

        // when
        final Set<Long> ready = provider.findReadyCallIds(
                List.of(both.getId(), one.getId(), none.getId()));

        // then
        assertThat(ready).containsExactly(both.getId());
    }

    @Test
    @DisplayName("callIds 가 비어있으면 빈 집합을 반환한다")
    void findReadyCallIds_whenEmpty_returnsEmpty() {
        // when & then
        assertThat(provider.findReadyCallIds(List.of())).isEmpty();
    }

    private Call saveEndedCall(final Long userA, final Long userB) {
        final Call call = callRepository.save(Call.start(userA, userB, UUID.randomUUID(), STARTED_AT));
        call.end(ENDED_AT);
        return callRepository.save(call);
    }

    private void saveRecording(final Long callId, final Long userId) {
        callRecordingRepository.save(CallRecording.upload(
                callId, userId, "call-recordings/%d/%d/key".formatted(callId, userId), "audio/m4a"));
    }
}
