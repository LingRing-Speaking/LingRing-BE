package com.lingring.domain.expression.domain;

import static jakarta.persistence.EnumType.STRING;
import static jakarta.persistence.GenerationType.IDENTITY;
import static lombok.AccessLevel.PROTECTED;

import com.lingring.domain.expression.domain.vo.Expression;
import com.lingring.domain.expression.domain.vo.Meaning;
import com.lingring.global.common.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;

@Entity
@Table(
        name = "user_expression",
        indexes = @Index(name = "idx_user_expression_user_created", columnList = "user_id, created_at"),
        uniqueConstraints = @UniqueConstraint(
                name = "uk_user_expression_bookmark",
                columnNames = {"user_id", "source", "source_ref_id", "source_sub_index"}
        )
)
@Getter
@NoArgsConstructor(access = PROTECTED)
public class UserExpression extends BaseTimeEntity {

    /**
     * 공유 소스(오늘의 추천·아이스브레이커)의 sourceSubIndex sentinel.
     * MySQL 유니크 제약은 NULL을 중복으로 보지 않으므로, dedupe가 작동하려면
     * null 대신 실존 인덱스가 될 수 없는 -1을 채운다 (0은 유효한 mistake 인덱스).
     */
    public static final int SHARED_SOURCE_SUB_INDEX = -1;

    @Id
    @GeneratedValue(strategy = IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Embedded
    private Expression expression;

    @Embedded
    private Meaning meaning;

    /**
     * 찜(북마크) 출처. 소스 기반 찜 도입(#182) 이전에 free-text로 저장된
     * 레거시 row는 세 컬럼 모두 null이다.
     */
    @Enumerated(STRING)
    @Column(name = "source", length = 32)
    private BookmarkSource source;

    /** 출처 참조 id. mistake는 analysisId, 공유 소스는 해당 엔티티의 id. */
    @Column(name = "source_ref_id")
    private Long sourceRefId;

    /** mistake의 결과 내 인덱스. 공유 소스는 {@link #SHARED_SOURCE_SUB_INDEX}. */
    @Column(name = "source_sub_index")
    private Integer sourceSubIndex;

    private UserExpression(
            @NonNull final Long userId,
            @NonNull final Expression expression,
            @NonNull final Meaning meaning
    ) {
        this.userId = userId;
        this.expression = expression;
        this.meaning = meaning;
    }

    /**
     * 출처 없는 표현 생성. 프로덕션 쓰기 경로는 {@link #bookmark}로 대체되었고,
     * 레거시(출처 없는) row 형태의 표현·테스트 시딩에만 쓰인다.
     */
    public static UserExpression create(
            @NonNull final Long userId,
            @NonNull final String expression,
            @NonNull final String meaning
    ) {
        return new UserExpression(userId, new Expression(expression), new Meaning(meaning));
    }

    /** 소스 기반 찜(북마크) 생성. 텍스트는 서버가 소스에서 도출해 전달한다. */
    public static UserExpression bookmark(
            @NonNull final Long userId,
            @NonNull final String expression,
            @NonNull final String meaning,
            @NonNull final BookmarkSource source,
            @NonNull final Long sourceRefId,
            final int sourceSubIndex
    ) {
        final UserExpression bookmarked =
                new UserExpression(userId, new Expression(expression), new Meaning(meaning));
        bookmarked.source = source;
        bookmarked.sourceRefId = sourceRefId;
        bookmarked.sourceSubIndex = sourceSubIndex;
        return bookmarked;
    }
}
