package com.lingring.domain.savedexpression.dao;

import static org.assertj.core.api.Assertions.assertThat;

import com.lingring.domain.savedexpression.domain.SavedExpression;
import com.lingring.global.config.RepositoryTestHelper;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;

class SavedExpressionRepositoryTest extends RepositoryTestHelper {

    @Autowired
    private SavedExpressionRepository savedExpressionRepository;

    @Nested
    @DisplayName("findAllByUserIdOrderByCreatedAtDesc")
    class FindAllByUserIdOrderByCreatedAtDesc {

        @Test
        @DisplayName("userId 필터링: 해당 사용자 항목만 반환한다")
        void returnsOnlyUsersItems() {
            // given
            final Long userId = 1L;
            final Long otherUserId = 2L;
            savedExpressionRepository.save(SavedExpression.create(userId, "first", "첫번째"));
            savedExpressionRepository.save(SavedExpression.create(userId, "second", "두번째"));
            savedExpressionRepository.save(SavedExpression.create(otherUserId, "other", "다른유저"));

            // when
            final Slice<SavedExpression> slice = savedExpressionRepository
                    .findAllByUserIdOrderByCreatedAtDesc(userId, PageRequest.of(0, 10));

            // then
            assertThat(slice.getContent()).hasSize(2);
            assertThat(slice.getContent()).allMatch(item -> item.getUserId().equals(userId));
            assertThat(slice.hasNext()).isFalse();
        }

        @Test
        @DisplayName("저장 건수보다 작은 size로 요청하면 hasNext가 true다")
        void hasNextTrueWhenMoreItemsRemain() {
            // given
            final Long userId = 1L;
            savedExpressionRepository.save(SavedExpression.create(userId, "first", "첫번째"));
            savedExpressionRepository.save(SavedExpression.create(userId, "second", "두번째"));
            savedExpressionRepository.save(SavedExpression.create(userId, "third", "세번째"));

            // when
            final Slice<SavedExpression> slice = savedExpressionRepository
                    .findAllByUserIdOrderByCreatedAtDesc(userId, PageRequest.of(0, 1));

            // then
            assertThat(slice.getContent()).hasSize(1);
            assertThat(slice.hasNext()).isTrue();
        }

        @Test
        @DisplayName("userId에 해당하는 항목이 없으면 빈 Slice 반환 + hasNext=false")
        void returnsEmptySliceWhenNone() {
            // given
            final Long missingUserId = 9_999_999L;

            // when
            final Slice<SavedExpression> slice = savedExpressionRepository
                    .findAllByUserIdOrderByCreatedAtDesc(missingUserId, PageRequest.of(0, 10));

            // then
            assertThat(slice.getContent()).isEmpty();
            assertThat(slice.hasNext()).isFalse();
        }
    }

    @Nested
    @DisplayName("findByIdAndUserId")
    class FindByIdAndUserId {

        @Test
        @DisplayName("id와 userId가 모두 일치하면 반환한다")
        void returnsWhenBothMatch() {
            // given
            final Long userId = 1L;
            final SavedExpression saved =
                    savedExpressionRepository.save(SavedExpression.create(userId, "hello", "안녕"));

            // when
            final Optional<SavedExpression> found =
                    savedExpressionRepository.findByIdAndUserId(saved.getId(), userId);

            // then
            assertThat(found).isPresent();
            assertThat(found.get().getId()).isEqualTo(saved.getId());
        }

        @Test
        @DisplayName("id는 일치하지만 userId가 다르면 Optional.empty()를 반환한다")
        void returnsEmptyWhenUserIdDiffers() {
            // given
            final Long userId = 1L;
            final Long otherUserId = 2L;
            final SavedExpression saved =
                    savedExpressionRepository.save(SavedExpression.create(userId, "hello", "안녕"));

            // when
            final Optional<SavedExpression> found =
                    savedExpressionRepository.findByIdAndUserId(saved.getId(), otherUserId);

            // then
            assertThat(found).isEmpty();
        }
    }
}
