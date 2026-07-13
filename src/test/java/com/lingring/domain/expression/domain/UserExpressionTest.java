package com.lingring.domain.expression.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.lingring.domain.expression.domain.vo.SourceSubIndex;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class UserExpressionTest {

    @Test
    @DisplayName("create는 userId와 VO로 래핑된 expression/meaning을 가진 엔티티를 생성한다 (출처 없음)")
    void createWrapsValuesIntoEntity() {
        // given
        final Long userId = 1L;
        final String expression = "How are you?";
        final String meaning = "어떻게 지내세요?";

        // when
        final UserExpression userExpression = UserExpression.create(userId, expression, meaning);

        // then
        assertThat(userExpression.getUserId()).isEqualTo(userId);
        assertThat(userExpression.getExpression().getValue()).isEqualTo(expression);
        assertThat(userExpression.getMeaning().getValue()).isEqualTo(meaning);
        assertThat(userExpression.getSource()).isNull();
        assertThat(userExpression.getSourceRefId()).isNull();
        assertThat(userExpression.getSourceSubIndex()).isNull();
    }

    @Test
    @DisplayName("bookmark는 텍스트와 함께 찜 출처(source/refId/subIndex)를 기록한다")
    void bookmarkRecordsSourceOrigin() {
        // given
        final Long userId = 1L;

        // when
        final UserExpression bookmarked = UserExpression.bookmark(
                userId, "do my homework", "숙제를 하다",
                BookmarkSource.ANALYSIS_MISTAKE, 42L, SourceSubIndex.mistakeIndex(1)
        );

        // then
        assertThat(bookmarked.getUserId()).isEqualTo(userId);
        assertThat(bookmarked.getExpression().getValue()).isEqualTo("do my homework");
        assertThat(bookmarked.getMeaning().getValue()).isEqualTo("숙제를 하다");
        assertThat(bookmarked.getSource()).isEqualTo(BookmarkSource.ANALYSIS_MISTAKE);
        assertThat(bookmarked.getSourceRefId()).isEqualTo(42L);
        assertThat(bookmarked.getSourceSubIndex()).isEqualTo(SourceSubIndex.mistakeIndex(1));
    }

    @Test
    @DisplayName("공유 소스(아이스브레이커 등)의 subIndex sentinel은 -1이다")
    void sharedSourceSubIndexSentinelIsMinusOne() {
        // when
        final UserExpression bookmarked = UserExpression.bookmark(
                1L, "How's it going?", "요즘 어때?",
                BookmarkSource.ICEBREAKER, 5L, SourceSubIndex.shared()
        );

        // then
        assertThat(bookmarked.getSourceSubIndex().getValue()).isEqualTo(-1);
    }
}
