package com.lingring.domain.user.dao;

import com.lingring.domain.user.domain.UserStats;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserStatsRepository extends JpaRepository<UserStats, Long> {

    Optional<UserStats> findByUserId(Long userId);
}
