package com.lingring.infrastructure.redis;

import static org.assertj.core.api.Assertions.assertThat;

import com.lingring.domain.matching.dao.MatchConfirmationRepository;
import com.lingring.domain.matching.dao.MatchConfirmationRepository.AcceptOutcome;
import com.lingring.domain.matching.dao.MatchConfirmationRepository.AcceptResult;
import com.lingring.domain.matching.dao.MatchingQueueRepository;
import com.lingring.domain.matching.domain.MatchConfirmation;
import com.lingring.domain.matching.domain.MatchingResult;
import com.lingring.global.config.ServiceIntegrationHelper;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class RedisMatchConfirmationRepositoryTest extends ServiceIntegrationHelper {

    private static final LocalDateTime ENQUEUED_AT = LocalDateTime.of(2026, 5, 12, 12, 0, 0);
    private static final LocalDateTime DEADLINE = LocalDateTime.of(2026, 5, 12, 12, 0, 15);
    private static final LocalDateTime BEFORE_DEADLINE = DEADLINE.minusSeconds(5);
    private static final LocalDateTime AFTER_DEADLINE = DEADLINE.plusSeconds(1);

    @Autowired
    private MatchConfirmationRepository repository;

    @Autowired
    private MatchingQueueRepository queueRepository;

    @Nested
    @DisplayName("commit: 큐 → confirm 전이")
    class Commit {

        @Test
        @DisplayName("양쪽 user가 큐에 있을 때 ZREM + confirm Hash 생성 + index 생성")
        void commit_success() {
            // given
            queueRepository.enqueue(1L, ENQUEUED_AT);
            queueRepository.enqueue(2L, ENQUEUED_AT);
            final UUID roomId = UUID.randomUUID();

            // when
            final boolean committed = repository.commit(1L, 2L, roomId, DEADLINE);

            // then
            assertThat(committed).isTrue();
            assertThat(queueRepository.contains(1L)).isFalse();
            assertThat(queueRepository.contains(2L)).isFalse();
            final Optional<MatchConfirmation> found = repository.findByUser(1L);
            assertThat(found).isPresent();
            assertThat(found.get().userAId()).isEqualTo(1L);
            assertThat(found.get().userBId()).isEqualTo(2L);
            assertThat(found.get().roomId()).isEqualTo(roomId);
        }

        @Test
        @DisplayName("한쪽이 큐에 없으면 커밋 실패하고 confirm은 만들어지지 않는다")
        void commit_whenOneSideNotInQueue_fails() {
            // given
            queueRepository.enqueue(1L, ENQUEUED_AT);

            // when
            final boolean committed = repository.commit(1L, 2L, UUID.randomUUID(), DEADLINE);

            // then
            assertThat(committed).isFalse();
            assertThat(repository.findByUser(1L)).isEmpty();
            assertThat(queueRepository.contains(1L)).isTrue();
        }
    }

    @Nested
    @DisplayName("accept: flag set + 양쪽 완료 시 result key promote")
    class Accept {

        @Test
        @DisplayName("한쪽만 accept하면 ACCEPTED_WAITING이며 confirm은 유지")
        void accept_oneSide_waiting() {
            // given
            seedCommittedConfirmation(1L, 2L, DEADLINE);

            // when
            final AcceptResult result = repository.accept(1L, BEFORE_DEADLINE);

            // then
            assertThat(result.outcome()).isEqualTo(AcceptOutcome.ACCEPTED_WAITING);
            assertThat(repository.findByUser(1L)).isPresent();
        }

        @Test
        @DisplayName("양쪽 모두 accept하면 MATCHED로 promote되고 result key가 생성되며 confirm은 삭제")
        void accept_bothSides_promotes() {
            // given
            final UUID roomId = seedCommittedConfirmation(1L, 2L, DEADLINE);

            // when
            repository.accept(1L, BEFORE_DEADLINE);
            final AcceptResult result = repository.accept(2L, BEFORE_DEADLINE);

            // then
            assertThat(result.outcome()).isEqualTo(AcceptOutcome.MATCHED);
            assertThat(repository.findByUser(1L)).isEmpty();
            assertThat(repository.findByUser(2L)).isEmpty();
            final Optional<MatchingResult> r1 = queueRepository.findResult(1L);
            final Optional<MatchingResult> r2 = queueRepository.findResult(2L);
            assertThat(r1).isPresent();
            assertThat(r1.get().partnerId()).isEqualTo(2L);
            assertThat(r1.get().roomId()).isEqualTo(roomId);
            assertThat(r2).isPresent();
            assertThat(r2.get().partnerId()).isEqualTo(1L);
        }

        @Test
        @DisplayName("deadline이 지난 후의 accept는 EXPIRED")
        void accept_afterDeadline_expired() {
            // given
            seedCommittedConfirmation(1L, 2L, DEADLINE);

            // when
            final AcceptResult result = repository.accept(1L, AFTER_DEADLINE);

            // then
            assertThat(result.outcome()).isEqualTo(AcceptOutcome.EXPIRED);
        }

        @Test
        @DisplayName("confirm 레코드 없는 사용자의 accept는 NOT_FOUND")
        void accept_noConfirmation_notFound() {
            // when
            final AcceptResult result = repository.accept(99L, BEFORE_DEADLINE);

            // then
            assertThat(result.outcome()).isEqualTo(AcceptOutcome.NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("delete: confirm + 양쪽 index 제거")
    class Delete {

        @Test
        @DisplayName("pairKey로 삭제하면 양쪽 user 색인까지 사라진다")
        void delete_removesAll() {
            // given
            seedCommittedConfirmation(1L, 2L, DEADLINE);

            // when
            repository.delete(MatchConfirmation.pairKeyOf(1L, 2L));

            // then
            assertThat(repository.findByUser(1L)).isEmpty();
            assertThat(repository.findByUser(2L)).isEmpty();
        }
    }

    @Nested
    @DisplayName("findAllExpired: 만료된 confirm 전체 조회")
    class FindAllExpired {

        @Test
        @DisplayName("now 시각 기준 deadline이 지난 confirm만 반환")
        void findAllExpired_returnsOnlyOverdue() {
            // given
            seedCommittedConfirmation(1L, 2L, DEADLINE.minusSeconds(10));    // expired
            seedCommittedConfirmation(3L, 4L, DEADLINE.plusSeconds(10));     // not yet

            // when
            final List<MatchConfirmation> expired = repository.findAllExpired(DEADLINE);

            // then
            assertThat(expired).hasSize(1);
            assertThat(expired.get(0).userAId()).isEqualTo(1L);
        }
    }

    private UUID seedCommittedConfirmation(final Long userA, final Long userB, final LocalDateTime deadline) {
        queueRepository.enqueue(userA, ENQUEUED_AT);
        queueRepository.enqueue(userB, ENQUEUED_AT);
        final UUID roomId = UUID.randomUUID();
        repository.commit(userA, userB, roomId, deadline);
        return roomId;
    }
}
