package com.lingring.domain.expression.dao;

import com.lingring.domain.expression.domain.RecommendedExpression;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RecommendedExpressionRepository extends JpaRepository<RecommendedExpression, Long> {
}
