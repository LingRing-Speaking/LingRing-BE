package com.lingring.domain.presence.service;

import com.lingring.domain.presence.dao.PresenceRepository;
import com.lingring.global.config.PresenceProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PresenceService {

    private final PresenceRepository presenceRepository;
    private final PresenceProperties presenceProperties;

    public void heartbeat(final Long userId) {
        presenceRepository.markOnline(userId, presenceProperties.ttl());
    }

    public void disconnect(final Long userId) {
        presenceRepository.markOffline(userId);
    }
}
