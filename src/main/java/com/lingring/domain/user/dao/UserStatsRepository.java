package com.lingring.domain.user.dao;

import com.lingring.domain.user.domain.UserStats;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface UserStatsRepository extends JpaRepository<UserStats, Long> {

    Optional<UserStats> findByUserId(Long userId);

    @Modifying
    @Query("UPDATE UserStats u "
            + "SET u.savedExpressionCount = u.savedExpressionCount + 1 "
            + "WHERE u.userId = :userId")
    int incrementSavedExpressionCount(@Param("userId") Long userId);

    @Modifying
    @Query("UPDATE UserStats u "
            + "SET u.savedExpressionCount = u.savedExpressionCount - 1 "
            + "WHERE u.userId = :userId")
    int decrementSavedExpressionCount(@Param("userId") Long userId);
}
