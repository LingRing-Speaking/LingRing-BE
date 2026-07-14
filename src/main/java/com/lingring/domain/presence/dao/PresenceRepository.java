package com.lingring.domain.presence.dao;

import java.time.Duration;
import java.util.Collection;
import java.util.Set;

public interface PresenceRepository {

    void markOnline(Long userId, Duration ttl);

    void markOffline(Long userId);

    Set<Long> findOnlineUserIds(Collection<Long> userIds);
}
