package com.lingring.domain.userexpression.dao;

import com.lingring.domain.userexpression.domain.UserExpression;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface UserExpressionRepository extends JpaRepository<UserExpression, Long> {

    Slice<UserExpression> findAllByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    Optional<UserExpression> findByIdAndUserId(Long id, Long userId);

    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM UserExpression ue WHERE ue.userId = :userId")
    int deleteByUserId(@Param("userId") Long userId);
}
