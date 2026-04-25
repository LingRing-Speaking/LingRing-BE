package com.lingring.domain.savedexpression.domain;

import static jakarta.persistence.GenerationType.IDENTITY;
import static lombok.AccessLevel.PROTECTED;

import com.lingring.domain.savedexpression.domain.vo.Expression;
import com.lingring.domain.savedexpression.domain.vo.Meaning;
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
        name = "saved_expression",
        indexes = @Index(name = "idx_saved_expression_user_created", columnList = "user_id, created_at")
)
@Getter
@NoArgsConstructor(access = PROTECTED)
public class SavedExpression extends BaseTimeEntity {

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

    private SavedExpression(
            @NonNull final Long userId,
            @NonNull final Expression expression,
            @NonNull final Meaning meaning
    ) {
        this.userId = userId;
        this.expression = expression;
        this.meaning = meaning;
    }

    public static SavedExpression create(
            @NonNull final Long userId,
            @NonNull final String expression,
            @NonNull final String meaning
    ) {
        return new SavedExpression(userId, new Expression(expression), new Meaning(meaning));
    }
}
