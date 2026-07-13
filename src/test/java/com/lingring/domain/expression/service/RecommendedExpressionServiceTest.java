package com.lingring.domain.expression.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.lingring.domain.expression.dao.RecommendedExpressionRepository;
import com.lingring.domain.expression.dao.UserExpressionRepository;
import com.lingring.domain.expression.domain.BookmarkSource;
import com.lingring.domain.expression.domain.RecommendedExpression;
import com.lingring.domain.expression.domain.UserExpression;
import com.lingring.domain.expression.dto.response.RecommendedExpressionResponse;
import com.lingring.global.config.ServiceIntegrationHelper;
import com.lingring.global.error.ErrorCode;
import com.lingring.global.error.exception.NotFoundException;
import com.lingring.global.util.FixedDateTimeProvider;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;

@Import(RecommendedExpressionServiceTest.FixedDateTimeProviderConfig.class)
class RecommendedExpressionServiceTest extends ServiceIntegrationHelper {

    private static final Long USER_ID = 1L;

    @Autowired
    private RecommendedExpressionService recommendedExpressionService;

    @Autowired
    private RecommendedExpressionRepository recommendedExpressionRepository;

    @Autowired
    private UserExpressionRepository userExpressionRepository;

    @Autowired
    private FixedDateTimeProvider fixedDateTimeProvider;

    @TestConfiguration
    static class FixedDateTimeProviderConfig {

        @Bean
        @Primary
        FixedDateTimeProvider dateTimeProvider() {
            return new FixedDateTimeProvider(LocalDateTime.of(2026, 4, 25, 0, 0));
        }
    }

    private void setDate(final LocalDate date) {
        fixedDateTimeProvider.setFixedTime(date.atStartOfDay());
    }

    @Nested
    @DisplayName("getDaily: 오늘의 추천 표현 조회")
    class GetDaily {

        @Test
        @DisplayName("DB가 비어있으면 RECOMMENDED_EXPRESSION_NOT_FOUND 예외가 발생한다")
        void getDaily_whenEmpty_throwsNotFound() {
            // given
            setDate(LocalDate.of(2026, 4, 25));

            // when & then
            assertThatThrownBy(() -> recommendedExpressionService.getDaily(USER_ID))
                    .isInstanceOf(NotFoundException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.RECOMMENDED_EXPRESSION_NOT_FOUND);
        }

        @Test
        @DisplayName("같은 날짜로 두 번 호출하면 동일한 id가 반환된다")
        void getDaily_isDeterministicWithinSameDay() {
            // given
            recommendedExpressionRepository.save(RecommendedExpression.create("a", "ㄱ"));
            recommendedExpressionRepository.save(RecommendedExpression.create("b", "ㄴ"));
            recommendedExpressionRepository.save(RecommendedExpression.create("c", "ㄷ"));
            setDate(LocalDate.of(2026, 4, 25));

            // when
            final RecommendedExpressionResponse first = recommendedExpressionService.getDaily(USER_ID);
            final RecommendedExpressionResponse second = recommendedExpressionService.getDaily(USER_ID);

            // then
            assertThat(first.id()).isEqualTo(second.id());
        }

        @Test
        @DisplayName("3건 등록 시 날짜에 따라 epochDay % 3 offset의 행이 반환된다")
        void getDaily_pickedByEpochDayModuloCount() {
            // given
            final List<RecommendedExpression> saved = List.of(
                    recommendedExpressionRepository.save(RecommendedExpression.create("a", "ㄱ")),
                    recommendedExpressionRepository.save(RecommendedExpression.create("b", "ㄴ")),
                    recommendedExpressionRepository.save(RecommendedExpression.create("c", "ㄷ"))
            );

            // when & then
            final LocalDate dateA = LocalDate.of(2026, 4, 25);
            setDate(dateA);
            final int offsetA = (int) Math.floorMod(dateA.toEpochDay(), 3L);
            assertThat(recommendedExpressionService.getDaily(USER_ID).id())
                    .isEqualTo(saved.get(offsetA).getId());

            final LocalDate dateB = dateA.plusDays(1);
            setDate(dateB);
            final int offsetB = (int) Math.floorMod(dateB.toEpochDay(), 3L);
            assertThat(recommendedExpressionService.getDaily(USER_ID).id())
                    .isEqualTo(saved.get(offsetB).getId());
        }

        @Test
        @DisplayName("1건만 있으면 어떤 날짜로 호출해도 그 1건이 반환된다")
        void getDaily_whenSingleRow_returnsThatRow() {
            // given
            final RecommendedExpression only = recommendedExpressionRepository.save(
                    RecommendedExpression.create("only", "유일"));
            setDate(LocalDate.of(2030, 1, 1));

            // when
            final RecommendedExpressionResponse response = recommendedExpressionService.getDaily(USER_ID);

            // then
            assertThat(response.id()).isEqualTo(only.getId());
            assertThat(response.expression()).isEqualTo("only");
            assertThat(response.meaning()).isEqualTo("유일");
        }
    }

    @Nested
    @DisplayName("getDaily: 찜 상태(bookmarkId) 노출")
    class GetDailyBookmarkId {

        @Test
        @DisplayName("찜했으면 bookmarkId가 채워지고, 안 찜했으면 null이다")
        void getDaily_reflectsCallerBookmark() {
            // given
            final RecommendedExpression only = recommendedExpressionRepository.save(
                    RecommendedExpression.create("only", "유일"));
            setDate(LocalDate.of(2026, 4, 25));
            assertThat(recommendedExpressionService.getDaily(USER_ID).bookmarkId()).isNull();

            final UserExpression bookmark = userExpressionRepository.save(UserExpression.bookmark(
                    USER_ID, "only", "유일",
                    BookmarkSource.DAILY_EXPRESSION, only.getId(),
                    UserExpression.SHARED_SOURCE_SUB_INDEX
            ));

            // when
            final RecommendedExpressionResponse response = recommendedExpressionService.getDaily(USER_ID);

            // then
            assertThat(response.bookmarkId()).isEqualTo(bookmark.getId());
        }

        @Test
        @DisplayName("다른 사용자의 찜은 내 bookmarkId에 반영되지 않는다")
        void getDaily_doesNotLeakOtherUsersBookmark() {
            // given
            final Long otherUserId = 2L;
            final RecommendedExpression only = recommendedExpressionRepository.save(
                    RecommendedExpression.create("only", "유일"));
            setDate(LocalDate.of(2026, 4, 25));
            userExpressionRepository.save(UserExpression.bookmark(
                    otherUserId, "only", "유일",
                    BookmarkSource.DAILY_EXPRESSION, only.getId(),
                    UserExpression.SHARED_SOURCE_SUB_INDEX
            ));

            // when
            final RecommendedExpressionResponse response = recommendedExpressionService.getDaily(USER_ID);

            // then
            assertThat(response.bookmarkId()).isNull();
        }
    }
}
