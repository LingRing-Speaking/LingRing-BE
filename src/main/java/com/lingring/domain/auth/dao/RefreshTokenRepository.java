package com.lingring.domain.auth.dao;

public interface RefreshTokenRepository {

    void save(Long userId, String refreshToken);

    boolean exists(Long userId);

    boolean matches(Long userId, String refreshToken);

    void deleteByUserId(Long userId);
}