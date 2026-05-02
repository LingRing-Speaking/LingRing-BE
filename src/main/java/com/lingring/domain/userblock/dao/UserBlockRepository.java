package com.lingring.domain.userblock.dao;

import com.lingring.domain.userblock.domain.UserBlock;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface UserBlockRepository extends JpaRepository<UserBlock, Long> {

    boolean existsByUserIdAndBlockedUserId(Long userId, Long blockedUserId);

    Optional<UserBlock> findByUserIdAndBlockedUserId(Long userId, Long blockedUserId);

    Slice<UserBlock> findAllByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    @Query("SELECT ub.blockedUserId FROM UserBlock ub WHERE ub.userId = :userId")
    List<Long> findBlockedUserIdsByUserId(@Param("userId") Long userId);

    @Query("SELECT ub.userId FROM UserBlock ub WHERE ub.blockedUserId = :blockedUserId")
    List<Long> findUserIdsByBlockedUserId(@Param("blockedUserId") Long blockedUserId);

    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM UserBlock ub WHERE ub.userId = :userId")
    int deleteByUserId(@Param("userId") Long userId);
}
