package com.lingring.domain.matching.scheduler;

import static org.mockito.BDDMockito.then;

import com.lingring.domain.matching.service.MatchingService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class MatchConfirmationExpiryWorkerTest {

    private final MatchingService matchingService = Mockito.mock(MatchingService.class);
    private final MatchConfirmationExpiryWorker worker = new MatchConfirmationExpiryWorker(matchingService);

    @Test
    @DisplayName("runOnce 호출 시 MatchingService.expireOverdueConfirmations 위임")
    void runOnce_delegates() {
        // when
        worker.runOnce();

        // then
        then(matchingService).should().expireOverdueConfirmations();
    }
}
