package com.lingring.domain.auth.dao;

import java.util.Optional;

public interface RefreshTokenRepository {

    void save(Long userId, String tokenHash);

    Optional<String> findByUserId(Long userId);

    void deleteByUserId(Long userId);
}
