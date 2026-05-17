package com.lingring.domain.matching.dao;

import java.time.Duration;

public interface PairCooldownRepository {

    void put(String pairKey, Duration ttl);

    boolean contains(String pairKey);
}
