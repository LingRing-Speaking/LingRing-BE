package com.lingring.domain.matching.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.lingring.domain.call.dao.CallRepository;
import com.lingring.domain.matching.dao.MatchConfirmationRepository;
import com.lingring.domain.matching.dao.MatchingQueueRepository;
import com.lingring.domain.matching.dao.PairCooldownRepository;
import com.lingring.domain.matching.domain.MatchConfirmation;
import com.lingring.domain.matching.domain.MatchingPollStatus;
import com.lingring.domain.matching.dto.response.MatchingStatusResponse;
import com.lingring.global.config.ServiceIntegrationHelper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class MatchingFlowIntegrationTest extends ServiceIntegrationHelper {

    @Autowired
    private MatchingService matchingService;

    @Autowired
    private MatchingExecutor matchingExecutor;

    @Autowired
    private MatchConfirmationRepository matchConfirmationRepository;

    @Autowired
    private PairCooldownRepository pairCooldownRepository;

    @Autowired
    private MatchingQueueRepository matchingQueueRepository;

    @Autowired
    private CallRepository callRepository;

    @Test
    @DisplayName("happy path: 양쪽 큐 입장 → 라운드 실행 → 양쪽 accept → MATCHED + Call 1건")
    void happyPath() {
        // given
        matchingService.enterQueue(1L);
        matchingService.enterQueue(2L);

        // when: 라운드 실행 → confirm 생성
        matchingExecutor.executeRound();

        // then: AWAITING_CONFIRM
        final MatchingStatusResponse before = matchingService.getStatus(1L);
        assertThat(before.status()).isEqualTo(MatchingPollStatus.AWAITING_CONFIRM);

        // when: 양쪽 accept
        matchingService.acceptMatch(1L);
        matchingService.acceptMatch(2L);

        // then: MATCHED + callId가 persist된 Call.id와 일치
        final MatchingStatusResponse after = matchingService.getStatus(1L);
        assertThat(after.status()).isEqualTo(MatchingPollStatus.MATCHED);
        assertThat(after.roomId()).isNotNull();
        assertThat(callRepository.count()).isEqualTo(1L);
        final Long persistedCallId = callRepository.findByRoomId(after.roomId())
                .orElseThrow()
                .getId();
        assertThat(after.callId()).isEqualTo(persistedCallId);
    }

    @Test
    @DisplayName("decline path: decline 시 양쪽 큐로 복귀하고 cooldown 적용, 같은 페어는 다음 라운드에서 매칭되지 않는다")
    void declinePath() {
        // given
        matchingService.enterQueue(1L);
        matchingService.enterQueue(2L);
        matchingExecutor.executeRound();

        // when
        matchingService.declineMatch(1L);

        // then
        assertThat(pairCooldownRepository.contains(MatchConfirmation.pairKeyOf(1L, 2L))).isTrue();
        assertThat(matchingQueueRepository.contains(1L)).isTrue();
        assertThat(matchingQueueRepository.contains(2L)).isTrue();

        // and: 다음 라운드에서도 같은 페어로는 매칭되지 않는다
        matchingExecutor.executeRound();
        assertThat(matchConfirmationRepository.findByUser(1L)).isEmpty();
        assertThat(matchingQueueRepository.contains(1L)).isTrue();
        assertThat(matchingQueueRepository.contains(2L)).isTrue();
    }
}
