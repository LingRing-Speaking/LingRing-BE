package com.lingring.domain.presence.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.lingring.domain.presence.dao.PresenceRepository;
import com.lingring.global.config.ServiceIntegrationHelper;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class PresenceServiceTest extends ServiceIntegrationHelper {

    @Autowired
    private PresenceService presenceService;

    @Autowired
    private PresenceRepository presenceRepository;

    @Test
    @DisplayName("heartbeat하면 온라인 상태가 된다")
    void heartbeat_marksUserOnline() {
        // when
        presenceService.heartbeat(1L);

        // then
        assertThat(presenceRepository.findOnlineUserIds(List.of(1L))).containsExactly(1L);
    }

    @Test
    @DisplayName("disconnect하면 즉시 오프라인 상태가 된다")
    void disconnect_marksUserOffline() {
        // given
        presenceService.heartbeat(1L);

        // when
        presenceService.disconnect(1L);

        // then
        assertThat(presenceRepository.findOnlineUserIds(List.of(1L))).isEmpty();
    }
}
