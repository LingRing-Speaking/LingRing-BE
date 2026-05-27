package com.lingring.domain.expression.domain;

import static jakarta.persistence.GenerationType.IDENTITY;
import static lombok.AccessLevel.PROTECTED;

import com.lingring.domain.expression.domain.vo.Expression;
import com.lingring.domain.expression.domain.vo.Meaning;
import com.lingring.global.common.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;

@Entity
@Table(
        name = "user_expression",
        indexes = @Index(name = "idx_user_expression_user_created", columnList = "user_id, created_at")
)
@Getter
@NoArgsConstructor(access = PROTECTED)
public class UserExpression extends BaseTimeEntity {

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

    private UserExpression(
            @NonNull final Long userId,
            @NonNull final Expression expression,
            @NonNull final Meaning meaning
    ) {
        this.userId = userId;
        this.expression = expression;
        this.meaning = meaning;
    }

    public static UserExpression create(
            @NonNull final Long userId,
            @NonNull final String expression,
            @NonNull final String meaning
    ) {
        return new UserExpression(userId, new Expression(expression), new Meaning(meaning));
    }
}
