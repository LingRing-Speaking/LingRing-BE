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
            + "SET u.expressionCount = u.expressionCount + 1 "
            + "WHERE u.userId = :userId")
    int incrementExpressionCount(@Param("userId") Long userId);

    @Modifying
    @Query("UPDATE UserStats u "
            + "SET u.expressionCount = u.expressionCount - 1 "
            + "WHERE u.userId = :userId")
    int decrementExpressionCount(@Param("userId") Long userId);

    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM UserStats u WHERE u.userId = :userId")
    int deleteByUserId(@Param("userId") Long userId);
}
