package com.lingring.domain.presence.service;

import com.lingring.domain.presence.dao.PresenceRepository;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PresenceService {

    // 클라이언트 하트비트 주기(5초)의 2배 — 하트비트 1회 유실까지 온라인 유지
    private static final Duration PRESENCE_TTL = Duration.ofSeconds(10);

    private final PresenceRepository presenceRepository;

    public void heartbeat(final Long userId) {
        presenceRepository.markOnline(userId, PRESENCE_TTL);
    }

    public void disconnect(final Long userId) {
        presenceRepository.markOffline(userId);
    }
}
