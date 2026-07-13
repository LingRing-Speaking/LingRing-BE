package com.lingring.domain.expression.dao;

import com.lingring.domain.expression.domain.BookmarkSource;
import com.lingring.domain.expression.domain.UserExpression;
import java.util.Collection;
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
public interface UserExpressionRepository extends JpaRepository<UserExpression, Long> {

    Slice<UserExpression> findAllByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    Optional<UserExpression> findByIdAndUserId(Long id, Long userId);

    Optional<UserExpression> findByUserIdAndSourceAndSourceRefIdAndSourceSubIndex(
            Long userId,
            BookmarkSource source,
            Long sourceRefId,
            Integer sourceSubIndex
    );

    /** 공유 소스(아이스브레이커 등) 목록 응답의 bookmarkId 벌크 조회용. */
    List<UserExpression> findAllByUserIdAndSourceAndSourceRefIdIn(
            Long userId,
            BookmarkSource source,
            Collection<Long> sourceRefIds
    );

    /** 한 분석(analysisId)의 mistake 찜 전체 — 분석 결과 응답의 bookmarkId 매핑용. */
    List<UserExpression> findAllByUserIdAndSourceAndSourceRefId(
            Long userId,
            BookmarkSource source,
            Long sourceRefId
    );

    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM UserExpression ue WHERE ue.userId = :userId")
    int deleteByUserId(@Param("userId") Long userId);
}
