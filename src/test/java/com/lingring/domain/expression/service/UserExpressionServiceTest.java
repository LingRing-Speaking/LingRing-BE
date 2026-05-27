package com.lingring.domain.expression.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.lingring.domain.expression.dao.UserExpressionRepository;
import com.lingring.domain.expression.domain.UserExpression;
import com.lingring.domain.expression.dto.request.UserExpressionCreateRequest;
import com.lingring.domain.expression.dto.response.UserExpressionListResponse;
import com.lingring.domain.expression.dto.response.UserExpressionResponse;
import com.lingring.domain.user.dao.UserStatsRepository;
import com.lingring.domain.user.domain.UserStats;
import com.lingring.global.config.ServiceIntegrationHelper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class UserExpressionServiceTest extends ServiceIntegrationHelper {

    @Autowired
    private UserExpressionService userExpressionService;

    @Autowired
    private UserExpressionRepository userExpressionRepository;

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
            final UserExpressionCreateRequest request =
                    new UserExpressionCreateRequest("How are you?", "어떻게 지내세요?");

            // when
            final UserExpressionResponse response = userExpressionService.save(userId, request);

            // then
            assertThat(response.id()).isNotNull();
            assertThat(response.userId()).isEqualTo(userId);
            assertThat(response.expression()).isEqualTo("How are you?");
            assertThat(response.meaning()).isEqualTo("어떻게 지내세요?");
            assertThat(userExpressionRepository.findById(response.id())).isPresent();
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
            userExpressionRepository.save(UserExpression.create(userId, "first", "첫번째"));
            userExpressionRepository.save(UserExpression.create(userId, "second", "두번째"));
            userExpressionRepository.save(UserExpression.create(userId, "third", "세번째"));

            // when
            final UserExpressionListResponse response =
                    userExpressionService.getAllByUserId(userId, 0, 2);

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
            final UserExpressionListResponse response =
                    userExpressionService.getAllByUserId(missingUserId, 0, 20);

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
                userExpressionRepository.save(UserExpression.create(userId, "e" + i, "뜻" + i));
            }

            // when
            final UserExpressionListResponse response =
                    userExpressionService.getAllByUserId(userId, 0, 999);

            // then
            assertThat(response.items()).hasSize(50);
            assertThat(response.hasNext()).isTrue();
        }

        @Test
        @DisplayName("size가 0이면 내부적으로 MIN_SIZE(1)로 clamp된다")
        void getAllByUserId_clampsZeroSizeToMin() {
            // given
            final Long userId = 1L;
            userExpressionRepository.save(UserExpression.create(userId, "first", "첫번째"));
            userExpressionRepository.save(UserExpression.create(userId, "second", "두번째"));

            // when
            final UserExpressionListResponse response =
                    userExpressionService.getAllByUserId(userId, 0, 0);

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
            final UserExpression saved =
                    userExpressionRepository.save(UserExpression.create(userId, "hello", "안녕"));

            // when
            userExpressionService.delete(userId, saved.getId());

            // then
            assertThat(userExpressionRepository.findById(saved.getId())).isEmpty();
        }

        @Test
        @DisplayName("다른 사용자의 항목을 지정하면 예외 없이 대상이 그대로 유지된다 (멱등)")
        void delete_whenNotOwned_keepsItemAndDoesNotThrow() {
            // given
            final Long userId = 1L;
            final Long otherUserId = 2L;
            final UserExpression saved =
                    userExpressionRepository.save(UserExpression.create(userId, "hello", "안녕"));

            // when
            userExpressionService.delete(otherUserId, saved.getId());

            // then
            assertThat(userExpressionRepository.findById(saved.getId())).isPresent();
        }

        @Test
        @DisplayName("존재하지 않는 id를 지정해도 예외 없이 완료된다 (멱등)")
        void delete_whenMissingId_doesNotThrow() {
            // given
            final Long userId = 1L;
            final Long missingId = 9_999_999L;

            // when & then
            userExpressionService.delete(userId, missingId);
        }
    }

    @Nested
    @DisplayName("UserStats.expressionCount 동기화")
    class CountSync {

        @Test
        @DisplayName("save 1회 호출 시 해당 유저의 expressionCount가 1 증가한다")
        void save_incrementsUserStatsCountByOne() {
            // given
            final Long userId = 1L;
            userStatsRepository.save(UserStats.create(userId));
            final UserExpressionCreateRequest request =
                    new UserExpressionCreateRequest("How are you?", "어떻게 지내세요?");

            // when
            userExpressionService.save(userId, request);

            // then
            final UserStats reloaded = userStatsRepository.findByUserId(userId).orElseThrow();
            assertThat(reloaded.getExpressionCount()).isEqualTo(1);
        }

        @Test
        @DisplayName("save 2회 호출 시 expressionCount가 누적되어 2가 된다")
        void save_twice_accumulatesCountToTwo() {
            // given
            final Long userId = 1L;
            userStatsRepository.save(UserStats.create(userId));

            // when
            userExpressionService.save(userId, new UserExpressionCreateRequest("hello", "안녕"));
            userExpressionService.save(userId, new UserExpressionCreateRequest("thanks", "고마워"));

            // then
            final UserStats reloaded = userStatsRepository.findByUserId(userId).orElseThrow();
            assertThat(reloaded.getExpressionCount()).isEqualTo(2);
        }

        @Test
        @DisplayName("본인 항목 delete 성공 시 expressionCount가 1 감소한다")
        void delete_whenOwnedByUser_decrementsUserStatsCountByOne() {
            // given
            final Long userId = 1L;
            userStatsRepository.save(UserStats.create(userId));
            final UserExpressionResponse saved = userExpressionService.save(
                    userId, new UserExpressionCreateRequest("hello", "안녕"));

            // when
            userExpressionService.delete(userId, saved.id());

            // then
            final UserStats reloaded = userStatsRepository.findByUserId(userId).orElseThrow();
            assertThat(reloaded.getExpressionCount()).isZero();
        }

        @Test
        @DisplayName("존재하지 않는 id로 delete를 호출해도 expressionCount는 변하지 않는다")
        void delete_whenMissingId_doesNotChangeUserStatsCount() {
            // given
            final Long userId = 1L;
            userStatsRepository.save(UserStats.create(userId));
            userExpressionService.save(userId, new UserExpressionCreateRequest("hello", "안녕"));
            final Long missingId = 9_999_999L;

            // when
            userExpressionService.delete(userId, missingId);

            // then
            final UserStats reloaded = userStatsRepository.findByUserId(userId).orElseThrow();
            assertThat(reloaded.getExpressionCount()).isEqualTo(1);
        }

        @Test
        @DisplayName("타 유저 소유 항목에 대한 delete는 양쪽 유저의 expressionCount를 변경하지 않는다")
        void delete_whenNotOwned_doesNotChangeAnyUserStatsCount() {
            // given
            final Long ownerId = 1L;
            final Long otherUserId = 2L;
            userStatsRepository.save(UserStats.create(ownerId));
            userStatsRepository.save(UserStats.create(otherUserId));
            final UserExpressionResponse saved = userExpressionService.save(
                    ownerId, new UserExpressionCreateRequest("hello", "안녕"));

            // when
            userExpressionService.delete(otherUserId, saved.id());

            // then
            final UserStats ownerStats = userStatsRepository.findByUserId(ownerId).orElseThrow();
            final UserStats otherStats = userStatsRepository.findByUserId(otherUserId).orElseThrow();
            assertThat(ownerStats.getExpressionCount()).isEqualTo(1);
            assertThat(otherStats.getExpressionCount()).isZero();
        }
    }
}
