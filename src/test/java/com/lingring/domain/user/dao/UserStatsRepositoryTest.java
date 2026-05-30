package com.lingring.domain.user.dao;

import static org.assertj.core.api.Assertions.assertThat;

import com.lingring.domain.user.domain.UserStats;
import com.lingring.global.config.RepositoryTestHelper;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class UserStatsRepositoryTest extends RepositoryTestHelper {

    @Autowired
    private UserStatsRepository userStatsRepository;

    @PersistenceContext
    private EntityManager em;

    @Nested
    @DisplayName("findByUserId")
    class FindByUserId {

        @Test
        @DisplayName("userId에 해당하는 UserStats가 있으면 반환한다")
        void findByUserId_whenExists_returnsUserStats() {
            // given
            final Long userId = 1L;
            final UserStats saved = userStatsRepository.save(UserStats.create(userId));

            // when
            final Optional<UserStats> found = userStatsRepository.findByUserId(userId);

            // then
            assertThat(found).isPresent();
            assertThat(found.get().getId()).isEqualTo(saved.getId());
            assertThat(found.get().getUserId()).isEqualTo(userId);
        }

        @Test
        @DisplayName("userId에 해당하는 UserStats가 없으면 Optional.empty()를 반환한다")
        void findByUserId_whenNotExists_returnsEmpty() {
            // given
            final Long missingUserId = 9_999_999L;

            // when
            final Optional<UserStats> found = userStatsRepository.findByUserId(missingUserId);

            // then
            assertThat(found).isEmpty();
        }
    }

    @Nested
    @DisplayName("incrementExpressionCount")
    class IncrementExpressionCount {

        @Test
        @DisplayName("userId에 해당하는 UserStats의 expressionCount를 1 증가시키고 영향 row 수 1을 반환한다")
        void incrementExpressionCount_whenExists_increasesByOne() {
            // given
            final Long userId = 1L;
            userStatsRepository.save(UserStats.create(userId));
            em.flush();
            em.clear();

            // when
            final int affected = userStatsRepository.incrementExpressionCount(userId);

            // then
            assertThat(affected).isEqualTo(1);
            final UserStats reloaded = userStatsRepository.findByUserId(userId).orElseThrow();
            assertThat(reloaded.getExpressionCount()).isEqualTo(1);
        }

        @Test
        @DisplayName("해당 userId의 UserStats가 없으면 영향 row 수 0을 반환하고 다른 row에 영향 없다")
        void incrementExpressionCount_whenNotExists_returnsZero() {
            // given
            final Long existingUserId = 1L;
            final Long missingUserId = 9_999_999L;
            userStatsRepository.save(UserStats.create(existingUserId));
            em.flush();
            em.clear();

            // when
            final int affected = userStatsRepository.incrementExpressionCount(missingUserId);

            // then
            assertThat(affected).isZero();
            final UserStats reloaded = userStatsRepository.findByUserId(existingUserId).orElseThrow();
            assertThat(reloaded.getExpressionCount()).isZero();
        }
    }

    @Nested
    @DisplayName("decrementExpressionCount")
    class DecrementExpressionCount {

        @Test
        @DisplayName("userId에 해당하는 UserStats의 expressionCount를 1 감소시키고 영향 row 수 1을 반환한다")
        void decrementExpressionCount_whenExists_decreasesByOne() {
            // given
            final Long userId = 1L;
            userStatsRepository.save(UserStats.create(userId));
            userStatsRepository.incrementExpressionCount(userId);
            userStatsRepository.incrementExpressionCount(userId);
            em.flush();
            em.clear();

            // when
            final int affected = userStatsRepository.decrementExpressionCount(userId);

            // then
            assertThat(affected).isEqualTo(1);
            final UserStats reloaded = userStatsRepository.findByUserId(userId).orElseThrow();
            assertThat(reloaded.getExpressionCount()).isEqualTo(1);
        }

        @Test
        @DisplayName("해당 userId의 UserStats가 없으면 영향 row 수 0을 반환하고 다른 row에 영향 없다")
        void decrementExpressionCount_whenNotExists_returnsZero() {
            // given
            final Long existingUserId = 1L;
            final Long missingUserId = 9_999_999L;
            userStatsRepository.save(UserStats.create(existingUserId));
            userStatsRepository.incrementExpressionCount(existingUserId);
            em.flush();
            em.clear();

            // when
            final int affected = userStatsRepository.decrementExpressionCount(missingUserId);

            // then
            assertThat(affected).isZero();
            final UserStats reloaded = userStatsRepository.findByUserId(existingUserId).orElseThrow();
            assertThat(reloaded.getExpressionCount()).isEqualTo(1);
        }
    }
}
