package com.lingring.domain.userblock.dao;

import com.lingring.domain.userblock.domain.UserBlock;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserBlockRepository extends JpaRepository<UserBlock, Long> {

    boolean existsByUserIdAndBlockedUserId(Long userId, Long blockedUserId);

    Optional<UserBlock> findByUserIdAndBlockedUserId(Long userId, Long blockedUserId);

    Slice<UserBlock> findAllByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);
}
