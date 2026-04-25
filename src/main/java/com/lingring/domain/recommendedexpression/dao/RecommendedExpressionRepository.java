package com.lingring.domain.recommendedexpression.dao;

import com.lingring.domain.recommendedexpression.domain.RecommendedExpression;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RecommendedExpressionRepository extends JpaRepository<RecommendedExpression, Long> {
}
