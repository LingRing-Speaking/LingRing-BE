package com.lingring.domain.user.dao;

import static org.assertj.core.api.Assertions.assertThat;

import com.lingring.domain.user.domain.UserStats;
import com.lingring.global.config.RepositoryTestHelper;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class UserStatsRepositoryTest extends RepositoryTestHelper {

    @Autowired
    private UserStatsRepository userStatsRepository;

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
}
