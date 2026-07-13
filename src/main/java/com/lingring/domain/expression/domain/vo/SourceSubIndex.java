package com.lingring.domain.expression.domain.vo;

import static lombok.AccessLevel.PROTECTED;

import com.lingring.global.error.ErrorCode;
import com.lingring.global.error.exception.InvalidValueException;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Embeddable
@Getter
@EqualsAndHashCode
@NoArgsConstructor(force = true, access = PROTECTED)
public class SourceSubIndex {

    // MySQL 유니크 제약은 NULL을 중복으로 보지 않으므로 공유 소스는 -1을 채워 dedupe한다
    private static final int SHARED = -1;

    @Column(name = "source_sub_index")
    private final Integer value;

    private SourceSubIndex(final int value) {
        this.value = value;
    }

    public static SourceSubIndex shared() {
        return new SourceSubIndex(SHARED);
    }

    public static SourceSubIndex mistakeIndex(final int index) {
        if (index < 0) {
            throw new InvalidValueException(
                    ErrorCode.INVALID_SOURCE_SUB_INDEX,
                    "mistake 인덱스는 0 이상이어야 합니다: %d".formatted(index)
            );
        }
        return new SourceSubIndex(index);
    }
}
