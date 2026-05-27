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
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;

@Entity
@Table(name = "recommended_expression")
@Getter
@NoArgsConstructor(access = PROTECTED)
public class RecommendedExpression extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Embedded
    private Expression expression;

    @Embedded
    private Meaning meaning;

    private RecommendedExpression(
            @NonNull final Expression expression,
            @NonNull final Meaning meaning
    ) {
        this.expression = expression;
        this.meaning = meaning;
    }

    public static RecommendedExpression create(
            @NonNull final String expression,
            @NonNull final String meaning
    ) {
        return new RecommendedExpression(new Expression(expression), new Meaning(meaning));
    }
}
