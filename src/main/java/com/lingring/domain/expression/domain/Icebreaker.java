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
@Table(name = "icebreaker")
@Getter
@NoArgsConstructor(access = PROTECTED)
public class Icebreaker extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Embedded
    private Expression expression;

    @Embedded
    private Meaning meaning;

    private Icebreaker(
            @NonNull final Expression expression,
            @NonNull final Meaning meaning
    ) {
        this.expression = expression;
        this.meaning = meaning;
    }

    public static Icebreaker create(
            @NonNull final String expression,
            @NonNull final String meaning
    ) {
        return new Icebreaker(new Expression(expression), new Meaning(meaning));
    }
}
