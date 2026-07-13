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

    // MySQL 유니크 제약은 NULL을 중복으로 보지 않으므로 공유 소스는 -1을 채워 dedupe한다
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

    @Enumerated(STRING)
    @Column(name = "source", length = 32)
    private BookmarkSource source;

    @Column(name = "source_ref_id")
    private Long sourceRefId;

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

    private UserExpression(
            @NonNull final Long userId,
            @NonNull final Expression expression,
            @NonNull final Meaning meaning,
            @NonNull final BookmarkSource source,
            @NonNull final Long sourceRefId,
            final int sourceSubIndex
    ) {
        this(userId, expression, meaning);
        this.source = source;
        this.sourceRefId = sourceRefId;
        this.sourceSubIndex = sourceSubIndex;
    }

    public static UserExpression create(
            @NonNull final Long userId,
            @NonNull final String expression,
            @NonNull final String meaning
    ) {
        return new UserExpression(userId, new Expression(expression), new Meaning(meaning));
    }

    public static UserExpression bookmark(
            @NonNull final Long userId,
            @NonNull final String expression,
            @NonNull final String meaning,
            @NonNull final BookmarkSource source,
            @NonNull final Long sourceRefId,
            final int sourceSubIndex
    ) {
        return new UserExpression(
                userId, new Expression(expression), new Meaning(meaning),
                source, sourceRefId, sourceSubIndex
        );
    }
}
