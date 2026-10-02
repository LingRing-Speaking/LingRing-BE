package com.lingring.domain.push.dao;

import com.lingring.domain.push.domain.DeviceToken;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DeviceTokenRepository extends JpaRepository<DeviceToken, Long> {

    Optional<DeviceToken> findByToken(String token);

    void deleteByUserIdAndToken(Long userId, String token);

    void deleteAllByUserId(Long userId);
}
