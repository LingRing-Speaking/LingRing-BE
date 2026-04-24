package com.lingring.domain.savedexpression.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.lingring.domain.savedexpression.dao.SavedExpressionRepository;
import com.lingring.domain.savedexpression.domain.SavedExpression;
import com.lingring.domain.savedexpression.dto.request.SavedExpressionCreateRequest;
import com.lingring.domain.savedexpression.dto.response.SavedExpressionListResponse;
import com.lingring.domain.savedexpression.dto.response.SavedExpressionResponse;
import com.lingring.domain.user.dao.UserStatsRepository;
import com.lingring.domain.user.domain.UserStats;
import com.lingring.global.config.ServiceIntegrationHelper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class SavedExpressionServiceTest extends ServiceIntegrationHelper {

    @Autowired
    private SavedExpressionService savedExpressionService;

    @Autowired
    private SavedExpressionRepository savedExpressionRepository;

    @Autowired
    private UserStatsRepository userStatsRepository;

    @Nested
    @DisplayName("save: 저장한 표현 생성")
    class Save {

        @Test
        @DisplayName("유효한 요청이면 저장 후 응답을 반환한다")
        void save_whenValidRequest_returnsResponse() {
            // given
            final Long userId = 1L;
            final SavedExpressionCreateRequest request =
                    new SavedExpressionCreateRequest("How are you?", "어떻게 지내세요?");

            // when
            final SavedExpressionResponse response = savedExpressionService.save(userId, request);

            // then
            assertThat(response.id()).isNotNull();
            assertThat(response.userId()).isEqualTo(userId);
            assertThat(response.expression()).isEqualTo("How are you?");
            assertThat(response.meaning()).isEqualTo("어떻게 지내세요?");
            assertThat(savedExpressionRepository.findById(response.id())).isPresent();
        }
    }

    @Nested
    @DisplayName("getAllByUserId: 저장한 표현 목록 조회")
    class GetAllByUserId {

        @Test
        @DisplayName("page=0, size=2로 3건 중 2건을 반환하고 hasNext=true")
        void getAllByUserId_returnsFirstPageWithHasNextTrue() {
            // given
            final Long userId = 1L;
            savedExpressionRepository.save(SavedExpression.create(userId, "first", "첫번째"));
            savedExpressionRepository.save(SavedExpression.create(userId, "second", "두번째"));
            savedExpressionRepository.save(SavedExpression.create(userId, "third", "세번째"));

            // when
            final SavedExpressionListResponse response =
                    savedExpressionService.getAllByUserId(userId, 0, 2);

            // then
            assertThat(response.items()).hasSize(2);
            assertThat(response.items()).allMatch(item -> item.userId().equals(userId));
            assertThat(response.hasNext()).isTrue();
        }

        @Test
        @DisplayName("해당 userId의 항목이 없으면 빈 items + hasNext=false")
        void getAllByUserId_whenNone_returnsEmptyWithHasNextFalse() {
            // given
            final Long missingUserId = 9_999_999L;

            // when
            final SavedExpressionListResponse response =
                    savedExpressionService.getAllByUserId(missingUserId, 0, 20);

            // then
            assertThat(response.items()).isEmpty();
            assertThat(response.hasNext()).isFalse();
        }

        @Test
        @DisplayName("size가 MAX_SIZE(50)를 초과하면 내부적으로 50으로 clamp된다")
        void getAllByUserId_clampsOversizedRequestToMax() {
            // given
            final Long userId = 1L;
            for (int i = 0; i < 51; i++) {
                savedExpressionRepository.save(SavedExpression.create(userId, "e" + i, "뜻" + i));
            }

            // when
            final SavedExpressionListResponse response =
                    savedExpressionService.getAllByUserId(userId, 0, 999);

            // then
            assertThat(response.items()).hasSize(50);
            assertThat(response.hasNext()).isTrue();
        }

        @Test
        @DisplayName("size가 0이면 내부적으로 MIN_SIZE(1)로 clamp된다")
        void getAllByUserId_clampsZeroSizeToMin() {
            // given
            final Long userId = 1L;
            savedExpressionRepository.save(SavedExpression.create(userId, "first", "첫번째"));
            savedExpressionRepository.save(SavedExpression.create(userId, "second", "두번째"));

            // when
            final SavedExpressionListResponse response =
                    savedExpressionService.getAllByUserId(userId, 0, 0);

            // then
            assertThat(response.items()).hasSize(1);
            assertThat(response.hasNext()).isTrue();
        }
    }

    @Nested
    @DisplayName("delete: 저장한 표현 삭제")
    class Delete {

        @Test
        @DisplayName("본인의 항목을 id로 지정하면 삭제된다")
        void delete_whenOwnedByUser_removesItem() {
            // given
            final Long userId = 1L;
            final SavedExpression saved =
                    savedExpressionRepository.save(SavedExpression.create(userId, "hello", "안녕"));

            // when
            savedExpressionService.delete(userId, saved.getId());

            // then
            assertThat(savedExpressionRepository.findById(saved.getId())).isEmpty();
        }

        @Test
        @DisplayName("다른 사용자의 항목을 지정하면 예외 없이 대상이 그대로 유지된다 (멱등)")
        void delete_whenNotOwned_keepsItemAndDoesNotThrow() {
            // given
            final Long userId = 1L;
            final Long otherUserId = 2L;
            final SavedExpression saved =
                    savedExpressionRepository.save(SavedExpression.create(userId, "hello", "안녕"));

            // when
            savedExpressionService.delete(otherUserId, saved.getId());

            // then
            assertThat(savedExpressionRepository.findById(saved.getId())).isPresent();
        }

        @Test
        @DisplayName("존재하지 않는 id를 지정해도 예외 없이 완료된다 (멱등)")
        void delete_whenMissingId_doesNotThrow() {
            // given
            final Long userId = 1L;
            final Long missingId = 9_999_999L;

            // when & then
            savedExpressionService.delete(userId, missingId);
        }
    }

    @Nested
    @DisplayName("UserStats.savedExpressionCount 동기화")
    class CountSync {

        @Test
        @DisplayName("save 1회 호출 시 해당 유저의 savedExpressionCount가 1 증가한다")
        void save_incrementsUserStatsCountByOne() {
            // given
            final Long userId = 1L;
            userStatsRepository.save(UserStats.create(userId));
            final SavedExpressionCreateRequest request =
                    new SavedExpressionCreateRequest("How are you?", "어떻게 지내세요?");

            // when
            savedExpressionService.save(userId, request);

            // then
            final UserStats reloaded = userStatsRepository.findByUserId(userId).orElseThrow();
            assertThat(reloaded.getSavedExpressionCount()).isEqualTo(1);
        }

        @Test
        @DisplayName("save 2회 호출 시 savedExpressionCount가 누적되어 2가 된다")
        void save_twice_accumulatesCountToTwo() {
            // given
            final Long userId = 1L;
            userStatsRepository.save(UserStats.create(userId));

            // when
            savedExpressionService.save(userId, new SavedExpressionCreateRequest("hello", "안녕"));
            savedExpressionService.save(userId, new SavedExpressionCreateRequest("thanks", "고마워"));

            // then
            final UserStats reloaded = userStatsRepository.findByUserId(userId).orElseThrow();
            assertThat(reloaded.getSavedExpressionCount()).isEqualTo(2);
        }

        @Test
        @DisplayName("본인 항목 delete 성공 시 savedExpressionCount가 1 감소한다")
        void delete_whenOwnedByUser_decrementsUserStatsCountByOne() {
            // given
            final Long userId = 1L;
            userStatsRepository.save(UserStats.create(userId));
            final SavedExpressionResponse saved = savedExpressionService.save(
                    userId, new SavedExpressionCreateRequest("hello", "안녕"));

            // when
            savedExpressionService.delete(userId, saved.id());

            // then
            final UserStats reloaded = userStatsRepository.findByUserId(userId).orElseThrow();
            assertThat(reloaded.getSavedExpressionCount()).isZero();
        }

        @Test
        @DisplayName("존재하지 않는 id로 delete를 호출해도 savedExpressionCount는 변하지 않는다")
        void delete_whenMissingId_doesNotChangeUserStatsCount() {
            // given
            final Long userId = 1L;
            userStatsRepository.save(UserStats.create(userId));
            savedExpressionService.save(userId, new SavedExpressionCreateRequest("hello", "안녕"));
            final Long missingId = 9_999_999L;

            // when
            savedExpressionService.delete(userId, missingId);

            // then
            final UserStats reloaded = userStatsRepository.findByUserId(userId).orElseThrow();
            assertThat(reloaded.getSavedExpressionCount()).isEqualTo(1);
        }

        @Test
        @DisplayName("타 유저 소유 항목에 대한 delete는 양쪽 유저의 savedExpressionCount를 변경하지 않는다")
        void delete_whenNotOwned_doesNotChangeAnyUserStatsCount() {
            // given
            final Long ownerId = 1L;
            final Long otherUserId = 2L;
            userStatsRepository.save(UserStats.create(ownerId));
            userStatsRepository.save(UserStats.create(otherUserId));
            final SavedExpressionResponse saved = savedExpressionService.save(
                    ownerId, new SavedExpressionCreateRequest("hello", "안녕"));

            // when
            savedExpressionService.delete(otherUserId, saved.id());

            // then
            final UserStats ownerStats = userStatsRepository.findByUserId(ownerId).orElseThrow();
            final UserStats otherStats = userStatsRepository.findByUserId(otherUserId).orElseThrow();
            assertThat(ownerStats.getSavedExpressionCount()).isEqualTo(1);
            assertThat(otherStats.getSavedExpressionCount()).isZero();
        }
    }
}
