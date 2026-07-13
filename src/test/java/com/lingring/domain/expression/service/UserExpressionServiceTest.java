package com.lingring.domain.expression.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.lingring.domain.expression.dao.IcebreakerRepository;
import com.lingring.domain.expression.dao.RecommendedExpressionRepository;
import com.lingring.domain.expression.dao.UserExpressionRepository;
import com.lingring.domain.expression.domain.BookmarkSource;
import com.lingring.domain.expression.domain.Icebreaker;
import com.lingring.domain.expression.domain.RecommendedExpression;
import com.lingring.domain.expression.domain.UserExpression;
import com.lingring.domain.expression.dto.request.BookmarkCreateRequest;
import com.lingring.domain.expression.dto.response.UserExpressionListResponse;
import com.lingring.domain.expression.dto.response.UserExpressionResponse;
import com.lingring.domain.review.dao.CallAnalysisRepository;
import com.lingring.domain.review.domain.analysis.CallAnalysis;
import com.lingring.domain.review.domain.analysis.vo.AnalysisResult;
import com.lingring.domain.review.domain.analysis.vo.FeedbackTag;
import com.lingring.domain.review.domain.analysis.vo.MistakeItem;
import com.lingring.domain.review.domain.analysis.vo.Mistakes;
import com.lingring.domain.review.domain.analysis.vo.Positives;
import com.lingring.domain.review.exception.CallAnalysisAccessForbiddenException;
import com.lingring.domain.review.exception.CallAnalysisNotFoundException;
import com.lingring.domain.user.dao.UserStatsRepository;
import com.lingring.domain.user.domain.UserStats;
import com.lingring.global.config.ServiceIntegrationHelper;
import com.lingring.global.error.exception.BadRequestException;
import com.lingring.global.error.exception.NotFoundException;
import java.util.List;
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

    @Autowired
    private IcebreakerRepository icebreakerRepository;

    @Autowired
    private RecommendedExpressionRepository recommendedExpressionRepository;

    @Autowired
    private CallAnalysisRepository callAnalysisRepository;

    private Icebreaker seedIcebreaker() {
        return icebreakerRepository.save(Icebreaker.create("How's it going?", "요즘 어때?"));
    }

    private RecommendedExpression seedDaily() {
        return recommendedExpressionRepository.save(
                RecommendedExpression.create("Sounds good to me.", "좋아요, 동의해요"));
    }

    private CallAnalysis seedCompletedAnalysis(final Long userId, final List<MistakeItem> mistakes) {
        final CallAnalysis analysis = CallAnalysis.processing(1L, userId);
        analysis.complete(
                new AnalysisResult(new Mistakes(mistakes), Positives.empty()),
                "test-model"
        );
        return callAnalysisRepository.save(analysis);
    }

    private static MistakeItem mistake(final String improved, final String koMeaning) {
        return new MistakeItem(FeedbackTag.GRAMMAR, "wrong text", improved, "이유", koMeaning);
    }

    private static BookmarkCreateRequest mistakeRequest(final Long analysisId, final Integer mistakeId) {
        return new BookmarkCreateRequest(BookmarkSource.ANALYSIS_MISTAKE, analysisId, mistakeId, null, null);
    }

    private static BookmarkCreateRequest dailyRequest(final Long recommendedExpressionId) {
        return new BookmarkCreateRequest(BookmarkSource.DAILY_EXPRESSION, null, null, recommendedExpressionId, null);
    }

    private static BookmarkCreateRequest icebreakerRequest(final Long icebreakerId) {
        return new BookmarkCreateRequest(BookmarkSource.ICEBREAKER, null, null, null, icebreakerId);
    }

    @Nested
    @DisplayName("save: 소스 기반 찜(북마크) 생성")
    class Save {

        @Test
        @DisplayName("ICEBREAKER 찜이면 아이스브레이커의 표현/뜻으로 저장한다")
        void save_whenIcebreakerSource_derivesTextFromIcebreaker() {
            // given
            final Long userId = 1L;
            final Icebreaker icebreaker = seedIcebreaker();

            // when
            final UserExpressionResponse response = userExpressionService.save(
                    userId, icebreakerRequest(icebreaker.getId()));

            // then
            assertThat(response.userId()).isEqualTo(userId);
            assertThat(response.expression()).isEqualTo("How's it going?");
            assertThat(response.meaning()).isEqualTo("요즘 어때?");
            final UserExpression saved = userExpressionRepository.findById(response.id()).orElseThrow();
            assertThat(saved.getSource()).isEqualTo(BookmarkSource.ICEBREAKER);
            assertThat(saved.getSourceRefId()).isEqualTo(icebreaker.getId());
            assertThat(saved.getSourceSubIndex()).isEqualTo(UserExpression.SHARED_SOURCE_SUB_INDEX);
        }

        @Test
        @DisplayName("DAILY_EXPRESSION 찜이면 추천 표현의 표현/뜻으로 저장한다")
        void save_whenDailySource_derivesTextFromRecommendedExpression() {
            // given
            final Long userId = 1L;
            final RecommendedExpression daily = seedDaily();

            // when
            final UserExpressionResponse response = userExpressionService.save(
                    userId, dailyRequest(daily.getId()));

            // then
            assertThat(response.expression()).isEqualTo("Sounds good to me.");
            assertThat(response.meaning()).isEqualTo("좋아요, 동의해요");
            final UserExpression saved = userExpressionRepository.findById(response.id()).orElseThrow();
            assertThat(saved.getSource()).isEqualTo(BookmarkSource.DAILY_EXPRESSION);
            assertThat(saved.getSourceRefId()).isEqualTo(daily.getId());
        }

        @Test
        @DisplayName("ANALYSIS_MISTAKE 찜이면 mistake의 improved/koMeaning으로 저장한다")
        void save_whenMistakeSource_derivesTextFromMistake() {
            // given
            final Long userId = 1L;
            final CallAnalysis analysis = seedCompletedAnalysis(userId, List.of(
                    mistake("I went to school yesterday.", "나는 어제 학교에 갔다"),
                    mistake("do my homework", "숙제를 하다")
            ));

            // when
            final UserExpressionResponse response = userExpressionService.save(
                    userId, mistakeRequest(analysis.getId(), 1));

            // then
            assertThat(response.expression()).isEqualTo("do my homework");
            assertThat(response.meaning()).isEqualTo("숙제를 하다");
            final UserExpression saved = userExpressionRepository.findById(response.id()).orElseThrow();
            assertThat(saved.getSource()).isEqualTo(BookmarkSource.ANALYSIS_MISTAKE);
            assertThat(saved.getSourceRefId()).isEqualTo(analysis.getId());
            assertThat(saved.getSourceSubIndex()).isEqualTo(1);
        }

        @Test
        @DisplayName("같은 소스를 다시 찜하면 새 row 없이 기존 응답을 반환한다 (멱등)")
        void save_whenDuplicateSource_returnsExistingWithoutNewRow() {
            // given
            final Long userId = 1L;
            final Icebreaker icebreaker = seedIcebreaker();
            final UserExpressionResponse first = userExpressionService.save(
                    userId, icebreakerRequest(icebreaker.getId()));

            // when
            final UserExpressionResponse second = userExpressionService.save(
                    userId, icebreakerRequest(icebreaker.getId()));

            // then
            assertThat(second.id()).isEqualTo(first.id());
            assertThat(userExpressionRepository.count()).isEqualTo(1);
        }

        @Test
        @DisplayName("같은 분석의 다른 mistake는 별도 row로 저장된다")
        void save_whenDifferentMistakeIndex_savesSeparateRows() {
            // given
            final Long userId = 1L;
            final CallAnalysis analysis = seedCompletedAnalysis(userId, List.of(
                    mistake("first improved", "첫번째"),
                    mistake("second improved", "두번째")
            ));

            // when
            final UserExpressionResponse first = userExpressionService.save(
                    userId, mistakeRequest(analysis.getId(), 0));
            final UserExpressionResponse second = userExpressionService.save(
                    userId, mistakeRequest(analysis.getId(), 1));

            // then
            assertThat(first.id()).isNotEqualTo(second.id());
            assertThat(userExpressionRepository.count()).isEqualTo(2);
        }

        @Test
        @DisplayName("같은 소스를 서로 다른 사용자가 찜하면 각각 저장된다")
        void save_whenSameSourceDifferentUsers_savesPerUser() {
            // given
            final Icebreaker icebreaker = seedIcebreaker();

            // when
            final UserExpressionResponse one = userExpressionService.save(
                    1L, icebreakerRequest(icebreaker.getId()));
            final UserExpressionResponse two = userExpressionService.save(
                    2L, icebreakerRequest(icebreaker.getId()));

            // then
            assertThat(one.id()).isNotEqualTo(two.id());
            assertThat(userExpressionRepository.count()).isEqualTo(2);
        }

        @Test
        @DisplayName("존재하지 않는 icebreakerId면 NotFoundException을 던진다")
        void save_whenIcebreakerMissing_throwsNotFound() {
            // given
            final Long missingId = 9_999_999L;

            // when & then
            assertThatThrownBy(() -> userExpressionService.save(
                    1L, icebreakerRequest(missingId)))
                    .isInstanceOf(NotFoundException.class);
        }

        @Test
        @DisplayName("존재하지 않는 recommendedExpressionId면 NotFoundException을 던진다")
        void save_whenDailyMissing_throwsNotFound() {
            // given
            final Long missingId = 9_999_999L;

            // when & then
            assertThatThrownBy(() -> userExpressionService.save(
                    1L, dailyRequest(missingId)))
                    .isInstanceOf(NotFoundException.class);
        }

        @Test
        @DisplayName("존재하지 않는 analysisId면 CallAnalysisNotFoundException을 던진다")
        void save_whenAnalysisMissing_throwsAnalysisNotFound() {
            // given
            final Long missingAnalysisId = 9_999_999L;

            // when & then
            assertThatThrownBy(() -> userExpressionService.save(
                    1L, mistakeRequest(missingAnalysisId, 0)))
                    .isInstanceOf(CallAnalysisNotFoundException.class);
        }

        @Test
        @DisplayName("남의 분석의 mistake를 찜하면 CallAnalysisAccessForbiddenException을 던진다")
        void save_whenAnalysisNotOwned_throwsForbidden() {
            // given
            final Long ownerId = 1L;
            final Long attackerId = 2L;
            final CallAnalysis analysis = seedCompletedAnalysis(ownerId, List.of(
                    mistake("improved", "뜻")
            ));

            // when & then
            assertThatThrownBy(() -> userExpressionService.save(
                    attackerId, mistakeRequest(analysis.getId(), 0)))
                    .isInstanceOf(CallAnalysisAccessForbiddenException.class);
        }

        @Test
        @DisplayName("mistakeId가 mistakes 범위를 벗어나면 NotFoundException을 던진다")
        void save_whenMistakeIndexOutOfRange_throwsNotFound() {
            // given
            final Long userId = 1L;
            final CallAnalysis analysis = seedCompletedAnalysis(userId, List.of(
                    mistake("improved", "뜻")
            ));

            // when & then
            assertThatThrownBy(() -> userExpressionService.save(
                    userId, mistakeRequest(analysis.getId(), 1)))
                    .isInstanceOf(NotFoundException.class);
        }

        @Test
        @DisplayName("아직 완료되지 않은(PROCESSING) 분석의 mistake를 찜하면 NotFoundException을 던진다")
        void save_whenAnalysisNotCompleted_throwsNotFound() {
            // given
            final Long userId = 1L;
            final CallAnalysis processing = callAnalysisRepository.save(
                    CallAnalysis.processing(1L, userId));

            // when & then
            assertThatThrownBy(() -> userExpressionService.save(
                    userId, mistakeRequest(processing.getId(), 0)))
                    .isInstanceOf(NotFoundException.class);
        }

        @Test
        @DisplayName("ANALYSIS_MISTAKE인데 analysisId가 없으면 BadRequestException을 던진다")
        void save_whenMistakeSourceWithoutAnalysisId_throwsBadRequest() {
            // when & then
            assertThatThrownBy(() -> userExpressionService.save(
                    1L, mistakeRequest(null, 0)))
                    .isInstanceOf(BadRequestException.class);
        }

        @Test
        @DisplayName("DAILY_EXPRESSION인데 recommendedExpressionId가 없으면 BadRequestException을 던진다")
        void save_whenDailySourceWithoutId_throwsBadRequest() {
            // when & then
            assertThatThrownBy(() -> userExpressionService.save(
                    1L, dailyRequest(null)))
                    .isInstanceOf(BadRequestException.class);
        }

        @Test
        @DisplayName("ICEBREAKER인데 icebreakerId가 없으면 BadRequestException을 던진다")
        void save_whenIcebreakerSourceWithoutId_throwsBadRequest() {
            // when & then
            assertThatThrownBy(() -> userExpressionService.save(
                    1L, icebreakerRequest(null)))
                    .isInstanceOf(BadRequestException.class);
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

        @Test
        @DisplayName("찜을 삭제한 뒤 같은 소스를 다시 찜하면 새 row로 저장된다")
        void delete_thenRebookmark_savesNewRow() {
            // given
            final Long userId = 1L;
            final Icebreaker icebreaker = seedIcebreaker();
            final UserExpressionResponse first = userExpressionService.save(
                    userId, icebreakerRequest(icebreaker.getId()));
            userExpressionService.delete(userId, first.id());

            // when
            final UserExpressionResponse second = userExpressionService.save(
                    userId, icebreakerRequest(icebreaker.getId()));

            // then
            assertThat(second.id()).isNotEqualTo(first.id());
            assertThat(userExpressionRepository.count()).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("UserStats.expressionCount 동기화")
    class CountSync {

        @Test
        @DisplayName("찜 1회 성공 시 해당 유저의 expressionCount가 1 증가한다")
        void save_incrementsUserStatsCountByOne() {
            // given
            final Long userId = 1L;
            userStatsRepository.save(UserStats.create(userId));
            final Icebreaker icebreaker = seedIcebreaker();

            // when
            userExpressionService.save(userId, icebreakerRequest(icebreaker.getId()));

            // then
            final UserStats reloaded = userStatsRepository.findByUserId(userId).orElseThrow();
            assertThat(reloaded.getExpressionCount()).isEqualTo(1);
        }

        @Test
        @DisplayName("서로 다른 소스 2건 찜 시 expressionCount가 누적되어 2가 된다")
        void save_twoDifferentSources_accumulatesCountToTwo() {
            // given
            final Long userId = 1L;
            userStatsRepository.save(UserStats.create(userId));
            final Icebreaker icebreaker = seedIcebreaker();
            final RecommendedExpression daily = seedDaily();

            // when
            userExpressionService.save(userId, icebreakerRequest(icebreaker.getId()));
            userExpressionService.save(userId, dailyRequest(daily.getId()));

            // then
            final UserStats reloaded = userStatsRepository.findByUserId(userId).orElseThrow();
            assertThat(reloaded.getExpressionCount()).isEqualTo(2);
        }

        @Test
        @DisplayName("같은 소스를 중복 찜하면 expressionCount는 1에서 변하지 않는다 (멱등)")
        void save_whenDuplicate_doesNotIncrementCount() {
            // given
            final Long userId = 1L;
            userStatsRepository.save(UserStats.create(userId));
            final Icebreaker icebreaker = seedIcebreaker();

            // when
            userExpressionService.save(userId, icebreakerRequest(icebreaker.getId()));
            userExpressionService.save(userId, icebreakerRequest(icebreaker.getId()));

            // then
            final UserStats reloaded = userStatsRepository.findByUserId(userId).orElseThrow();
            assertThat(reloaded.getExpressionCount()).isEqualTo(1);
        }

        @Test
        @DisplayName("본인 항목 delete 성공 시 expressionCount가 1 감소한다")
        void delete_whenOwnedByUser_decrementsUserStatsCountByOne() {
            // given
            final Long userId = 1L;
            userStatsRepository.save(UserStats.create(userId));
            final Icebreaker icebreaker = seedIcebreaker();
            final UserExpressionResponse saved = userExpressionService.save(
                    userId, icebreakerRequest(icebreaker.getId()));

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
            final Icebreaker icebreaker = seedIcebreaker();
            userExpressionService.save(userId, icebreakerRequest(icebreaker.getId()));
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
            final Icebreaker icebreaker = seedIcebreaker();
            final UserExpressionResponse saved = userExpressionService.save(
                    ownerId, icebreakerRequest(icebreaker.getId()));

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
