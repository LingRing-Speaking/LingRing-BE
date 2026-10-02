package com.lingring.domain.push.dao;

import com.lingring.domain.push.domain.DeviceToken;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface DeviceTokenRepository extends JpaRepository<DeviceToken, Long> {

    Optional<DeviceToken> findByToken(String token);

    List<DeviceToken> findAllByUserId(Long userId);

    @Query("""
            SELECT d
            FROM DeviceToken d
            JOIN User u ON u.id = d.userId
            WHERE u.marketingPushConsent.agreed = true
              AND d.id > :lastId
            ORDER BY d.id
            """)
    List<DeviceToken> findDailyReminderTargets(@Param("lastId") Long lastId, Limit limit);

    void deleteByUserIdAndToken(Long userId, String token);

    void deleteAllByUserId(Long userId);

    @Transactional
    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM DeviceToken d WHERE d.token IN :tokens")
    int deleteAllByTokenIn(@Param("tokens") Collection<String> tokens);
}
