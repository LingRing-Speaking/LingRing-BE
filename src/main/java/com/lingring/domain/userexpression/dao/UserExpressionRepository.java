package com.lingring.domain.savedexpression.dao;

import com.lingring.domain.savedexpression.domain.SavedExpression;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SavedExpressionRepository extends JpaRepository<SavedExpression, Long> {

    Slice<SavedExpression> findAllByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    Optional<SavedExpression> findByIdAndUserId(Long id, Long userId);
}
