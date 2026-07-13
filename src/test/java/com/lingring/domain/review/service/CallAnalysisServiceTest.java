package com.lingring.domain.review.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.lingring.domain.call.dao.CallRepository;
import com.lingring.domain.call.domain.Call;
import com.lingring.domain.call.dto.response.CallAnalysisStatusView;
import com.lingring.domain.expression.dao.UserExpressionRepository;
import com.lingring.domain.expression.domain.BookmarkSource;
import com.lingring.domain.expression.domain.UserExpression;
import com.lingring.domain.review.dao.CallAnalysisRepository;
import com.lingring.domain.review.domain.analysis.CallAnalysis;
import com.lingring.domain.review.domain.analysis.CallAnalysisStatus;
import com.lingring.domain.review.domain.analysis.vo.AnalysisResult;
import com.lingring.domain.review.domain.analysis.vo.FeedbackTag;
import com.lingring.domain.review.domain.analysis.vo.MistakeItem;
import com.lingring.domain.review.domain.analysis.vo.Mistakes;
import com.lingring.domain.review.domain.analysis.vo.PositiveItem;
import com.lingring.domain.review.domain.analysis.vo.Positives;
import com.lingring.domain.review.dto.response.CallAnalysisResponse;
import com.lingring.domain.review.dto.response.CallAnalysisStatusResponse;
import com.lingring.domain.review.exception.CallAnalysisAccessForbiddenException;
import com.lingring.domain.review.exception.CallAnalysisNotFoundException;
import com.lingring.global.config.ServiceIntegrationHelper;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class CallAnalysisServiceTest extends ServiceIntegrationHelper {

    private static final Long CALL_ID = 100L;
    private static final Long USER_ID = 1L;
    private static final Long OTHER_USER_ID = 2L;
    private static final String MODEL = "gemini-2.5-flash";

    @Autowired
    private CallAnalysisService callAnalysisService;

    @Autowired
    private CallAnalysisRepository callAnalysisRepository;

    @Autowired
    private CallRepository callRepository;

    @Autowired
    private UserExpressionRepository userExpressionRepository;

    @Nested
    @DisplayName("requestForUser: 호출자 본인 행 생성 + requested 마킹 + 최초 전환 여부 반환")
    class RequestForUser {

        @Test
        @DisplayName("기존 행이 없으면 PROCESSING + requested=true 로 저장하고 freshlyRequested=true 다")
        void requestForUser_whenNotExists_createsRowAndMarksRequested() {
            // when
            final RequestResult result = callAnalysisService.requestForUser(CALL_ID, USER_ID);

            // then
            assertThat(result.analysis().getId()).isNotNull();
            assertThat(result.analysis().getStatus()).isEqualTo(CallAnalysisStatus.PROCESSING);
            assertThat(result.analysis().isRequested()).isTrue();
            assertThat(result.freshlyRequested()).isTrue();
        }

        @Test
        @DisplayName("기존 행이 requested=false 였으면 requested=true 로 갱신하고 freshlyRequested=true 다")
        void requestForUser_whenExistsWithRequestedFalse_flipsToTrue() {
            // given: 짝꿍 placeholder처럼 미리 ensureExistsForUser 로 만들어둠 (requested=false)
            final CallAnalysis placeholder = callAnalysisService.ensureExistsForUser(CALL_ID, USER_ID);
            assertThat(placeholder.isRequested()).isFalse();

            // when
            final RequestResult result = callAnalysisService.requestForUser(CALL_ID, USER_ID);

            // then
            assertThat(result.analysis().getId()).isEqualTo(placeholder.getId());
            assertThat(result.analysis().isRequested()).isTrue();
            assertThat(result.freshlyRequested()).isTrue();
        }

        @Test
        @DisplayName("이미 requested=true 인 행에 다시 호출하면 freshlyRequested=false 다 (멱등)")
        void requestForUser_whenAlreadyRequested_isIdempotent() {
            // given
            final RequestResult first = callAnalysisService.requestForUser(CALL_ID, USER_ID);

            // when
            final RequestResult second = callAnalysisService.requestForUser(CALL_ID, USER_ID);

            // then
            assertThat(second.analysis().getId()).isEqualTo(first.analysis().getId());
            assertThat(second.analysis().isRequested()).isTrue();
            assertThat(second.freshlyRequested()).isFalse();
        }
    }

    @Nested
    @DisplayName("ensureExistsForUser: 짝꿍 placeholder (requested 변경 안 함)")
    class EnsureExistsForUser {

        @Test
        @DisplayName("기존 행이 없으면 PROCESSING + requested=false 로 새 행을 저장한다")
        void ensureExistsForUser_whenNotExists_createsRowAsNotRequested() {
            // when
            final CallAnalysis saved = callAnalysisService.ensureExistsForUser(CALL_ID, USER_ID);

            // then
            assertThat(saved.isRequested()).isFalse();
        }

        @Test
        @DisplayName("이미 requested=true 인 행이 있으면 그대로 두고 requested 를 내리지 않는다")
        void ensureExistsForUser_whenAlreadyRequested_doesNotResetFlag() {
            // given
            callAnalysisService.requestForUser(CALL_ID, USER_ID);

            // when
            final CallAnalysis result = callAnalysisService.ensureExistsForUser(CALL_ID, USER_ID);

            // then
            assertThat(result.isRequested()).isTrue();
        }
    }

    @Nested
    @DisplayName("complete: PROCESSING → COMPLETED 전이")
    class Complete {

        @Test
        @DisplayName("PROCESSING 상태에서 COMPLETED로 전이하고 result/modelIdentifier가 채워진다")
        void complete_whenProcessing_transitions() {
            // given: 짝꿍의 placeholder도 complete 가능해야 함 (requested 무관)
            callAnalysisService.ensureExistsForUser(CALL_ID, USER_ID);

            // when
            callAnalysisService.complete(CALL_ID, USER_ID, sampleResult(), MODEL);

            // then
            final CallAnalysis saved = callAnalysisRepository.findByCallIdAndUserId(CALL_ID, USER_ID).orElseThrow();
            assertThat(saved.getStatus()).isEqualTo(CallAnalysisStatus.COMPLETED);
            assertThat(saved.getModelIdentifier()).isEqualTo(MODEL);
            assertThat(saved.getResult().mistakes().count()).isEqualTo(1);
            assertThat(saved.getResult().positives().count()).isEqualTo(1);
        }

        @Test
        @DisplayName("이미 COMPLETED 상태에서 다시 호출하면 무시된다 (멱등)")
        void complete_whenAlreadyCompleted_isIdempotent() {
            // given
            callAnalysisService.requestForUser(CALL_ID, USER_ID);
            callAnalysisService.complete(CALL_ID, USER_ID, sampleResult(), MODEL);

            // when
            callAnalysisService.complete(CALL_ID, USER_ID, AnalysisResult.empty(), "different-model");

            // then
            final CallAnalysis saved = callAnalysisRepository.findByCallIdAndUserId(CALL_ID, USER_ID).orElseThrow();
            assertThat(saved.getModelIdentifier()).isEqualTo(MODEL);
            assertThat(saved.getResult().mistakes().count()).isEqualTo(1);
        }

        @Test
        @DisplayName("행이 없으면 CallAnalysisNotFoundException")
        void complete_whenMissing_throws() {
            // when & then
            assertThatThrownBy(() ->
                    callAnalysisService.complete(CALL_ID, USER_ID, sampleResult(), MODEL))
                    .isInstanceOf(CallAnalysisNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("fail: PROCESSING → FAILED 전이")
    class Fail {

        @Test
        @DisplayName("PROCESSING 상태에서 FAILED로 전이한다")
        void fail_whenProcessing_transitions() {
            // given
            callAnalysisService.requestForUser(CALL_ID, USER_ID);

            // when
            callAnalysisService.fail(CALL_ID, USER_ID);

            // then
            assertThat(callAnalysisRepository.findByCallIdAndUserId(CALL_ID, USER_ID).orElseThrow().getStatus())
                    .isEqualTo(CallAnalysisStatus.FAILED);
        }

        @Test
        @DisplayName("행이 없으면 예외 없이 무시된다")
        void fail_whenMissing_doesNothing() {
            // when & then (no throw)
            callAnalysisService.fail(CALL_ID, USER_ID);
        }
    }

    @Nested
    @DisplayName("getStatus: 본인 분석 + requested 만 노출")
    class GetStatus {

        @Test
        @DisplayName("본인 소유 + requested=true 면 status를 반환한다")
        void getStatus_whenOwnedAndRequested_returnsStatus() {
            // given
            final Long callId = saveEndedCall(USER_ID, OTHER_USER_ID, LocalDateTime.now().minusDays(1));
            final CallAnalysis saved = callAnalysisService.requestForUser(callId, USER_ID).analysis();

            // when
            final CallAnalysisStatusResponse response = callAnalysisService.getStatus(saved.getId(), USER_ID);

            // then
            assertThat(response.status()).isEqualTo(CallAnalysisStatusView.PROCESSING);
        }

        @Test
        @DisplayName("FAILED 분석은 녹음 보관 기간(30일)이 지나면 EXPIRED 로 노출된다")
        void getStatus_whenFailedAndExpired_returnsExpired() {
            // given: 30일을 넘긴 통화의 실패한 분석
            final Long callId = saveEndedCall(USER_ID, OTHER_USER_ID, LocalDateTime.now().minusDays(31));
            final CallAnalysis saved = callAnalysisService.requestForUser(callId, USER_ID).analysis();
            callAnalysisService.fail(callId, USER_ID);

            // when
            final CallAnalysisStatusResponse response = callAnalysisService.getStatus(saved.getId(), USER_ID);

            // then
            assertThat(response.status()).isEqualTo(CallAnalysisStatusView.EXPIRED);
        }

        @Test
        @DisplayName("본인 소유여도 requested=false 면 NotFound (숨김)")
        void getStatus_whenOwnedButNotRequested_throwsNotFound() {
            // given: 짝꿍이 만들어둔 placeholder 처럼 requested=false
            final CallAnalysis placeholder = callAnalysisService.ensureExistsForUser(CALL_ID, USER_ID);

            // when & then
            assertThatThrownBy(() -> callAnalysisService.getStatus(placeholder.getId(), USER_ID))
                    .isInstanceOf(CallAnalysisNotFoundException.class);
        }

        @Test
        @DisplayName("존재하지 않는 analysisId 면 NotFound")
        void getStatus_whenMissing_throws() {
            // when & then
            assertThatThrownBy(() -> callAnalysisService.getStatus(9999L, USER_ID))
                    .isInstanceOf(CallAnalysisNotFoundException.class);
        }

        @Test
        @DisplayName("타인 소유 analysisId 조회 시 Forbidden")
        void getStatus_whenNotOwned_throwsForbidden() {
            // given
            final CallAnalysis other = callAnalysisService.requestForUser(CALL_ID, OTHER_USER_ID).analysis();

            // when & then
            assertThatThrownBy(() -> callAnalysisService.getStatus(other.getId(), USER_ID))
                    .isInstanceOf(CallAnalysisAccessForbiddenException.class);
        }
    }

    @Nested
    @DisplayName("get: 분석 결과 조회 (analysisId + requested 게이팅)")
    class Get {

        @Test
        @DisplayName("본인 소유 + requested=true 면 응답 DTO 반환")
        void get_whenOwnedAndRequested_returnsResponse() {
            // given
            final CallAnalysis saved = callAnalysisService.requestForUser(CALL_ID, USER_ID).analysis();
            callAnalysisService.complete(CALL_ID, USER_ID, sampleResult(), MODEL);

            // when
            final CallAnalysisResponse response = callAnalysisService.get(saved.getId(), USER_ID);

            // then
            assertThat(response.callId()).isEqualTo(CALL_ID);
            assertThat(response.userId()).isEqualTo(USER_ID);
            assertThat(response.status()).isEqualTo(CallAnalysisStatus.COMPLETED);
            assertThat(response.mistakes()).hasSize(1);
            assertThat(response.positives()).hasSize(1);
        }

        @Test
        @DisplayName("mistakes 항목에는 결과 내 인덱스가 id로 부여된다")
        void get_assignsIndexAsMistakeId() {
            // given
            final CallAnalysis saved = callAnalysisService.requestForUser(CALL_ID, USER_ID).analysis();
            callAnalysisService.complete(CALL_ID, USER_ID, twoMistakesResult(), MODEL);

            // when
            final CallAnalysisResponse response = callAnalysisService.get(saved.getId(), USER_ID);

            // then
            assertThat(response.mistakes()).hasSize(2);
            assertThat(response.mistakes().get(0).id()).isEqualTo(0);
            assertThat(response.mistakes().get(1).id()).isEqualTo(1);
        }

        @Test
        @DisplayName("찜한 mistake는 bookmarkId가 채워지고, 안 찜한 것은 null이다")
        void get_marksBookmarkedMistakesOnly() {
            // given
            final CallAnalysis saved = callAnalysisService.requestForUser(CALL_ID, USER_ID).analysis();
            callAnalysisService.complete(CALL_ID, USER_ID, twoMistakesResult(), MODEL);
            final UserExpression bookmark = userExpressionRepository.save(UserExpression.bookmark(
                    USER_ID, "I go", "나는 간다",
                    BookmarkSource.ANALYSIS_MISTAKE, saved.getId(), 1
            ));

            // when
            final CallAnalysisResponse response = callAnalysisService.get(saved.getId(), USER_ID);

            // then
            assertThat(response.mistakes().get(0).bookmarkId()).isNull();
            assertThat(response.mistakes().get(1).bookmarkId()).isEqualTo(bookmark.getId());
        }

        @Test
        @DisplayName("다른 사용자의 찜은 내 응답의 bookmarkId에 반영되지 않는다")
        void get_doesNotLeakOtherUsersBookmarks() {
            // given: 같은 통화의 상대방(OTHER_USER_ID)이 자기 분석의 mistake를 찜한 상황
            final CallAnalysis mine = callAnalysisService.requestForUser(CALL_ID, USER_ID).analysis();
            callAnalysisService.complete(CALL_ID, USER_ID, twoMistakesResult(), MODEL);
            userExpressionRepository.save(UserExpression.bookmark(
                    OTHER_USER_ID, "I go", "나는 간다",
                    BookmarkSource.ANALYSIS_MISTAKE, mine.getId(), 1
            ));

            // when
            final CallAnalysisResponse response = callAnalysisService.get(mine.getId(), USER_ID);

            // then
            assertThat(response.mistakes().get(1).bookmarkId()).isNull();
        }

        @Test
        @DisplayName("본인 소유여도 requested=false 면 NotFound (숨김)")
        void get_whenOwnedButNotRequested_throwsNotFound() {
            // given
            final CallAnalysis placeholder = callAnalysisService.ensureExistsForUser(CALL_ID, USER_ID);

            // when & then
            assertThatThrownBy(() -> callAnalysisService.get(placeholder.getId(), USER_ID))
                    .isInstanceOf(CallAnalysisNotFoundException.class);
        }

        @Test
        @DisplayName("존재하지 않는 analysisId 면 NotFound")
        void get_whenMissing_throws() {
            // when & then
            assertThatThrownBy(() -> callAnalysisService.get(9999L, USER_ID))
                    .isInstanceOf(CallAnalysisNotFoundException.class);
        }

        @Test
        @DisplayName("타인 소유 analysisId 조회 시 Forbidden")
        void get_whenNotOwned_throwsForbidden() {
            // given
            final CallAnalysis other = callAnalysisService.requestForUser(CALL_ID, OTHER_USER_ID).analysis();

            // when & then
            assertThatThrownBy(() -> callAnalysisService.get(other.getId(), USER_ID))
                    .isInstanceOf(CallAnalysisAccessForbiddenException.class);
        }
    }

    @Nested
    @DisplayName("findRequestedAnalysisSummariesByCallIds: 통화 목록 enrichment 용 일괄 조회")
    class FindRequestedAnalysisSummariesByCallIds {

        @Test
        @DisplayName("requested=true 인 본인 행만 callId → (analysisId, status) 매핑으로 반환한다")
        void findRequestedAnalysisSummariesByCallIds_returnsOnlyRequestedOwnRows() {
            // given
            final CallAnalysis a1 = callAnalysisService.requestForUser(101L, USER_ID).analysis();   // mine, requested
            callAnalysisService.ensureExistsForUser(102L, USER_ID);                       // mine, NOT requested
            callAnalysisService.requestForUser(101L, OTHER_USER_ID);                      // other user — should be excluded

            // when
            final Map<Long, CallAnalysisSummary> result =
                    callAnalysisService.findRequestedAnalysisSummariesByCallIds(
                            USER_ID, List.of(101L, 102L, 999L));

            // then
            assertThat(result).hasSize(1);
            assertThat(result.get(101L).analysisId()).isEqualTo(a1.getId());
            assertThat(result.get(101L).status()).isEqualTo(CallAnalysisStatus.PROCESSING);
        }

        @Test
        @DisplayName("분석이 완료되면 status 가 COMPLETED 로 반환된다")
        void findRequestedAnalysisSummariesByCallIds_reflectsCompletedStatus() {
            // given
            final CallAnalysis a1 = callAnalysisService.requestForUser(101L, USER_ID).analysis();
            callAnalysisService.complete(101L, USER_ID, sampleResult(), MODEL);

            // when
            final Map<Long, CallAnalysisSummary> result =
                    callAnalysisService.findRequestedAnalysisSummariesByCallIds(
                            USER_ID, List.of(101L));

            // then
            assertThat(result.get(101L).analysisId()).isEqualTo(a1.getId());
            assertThat(result.get(101L).status()).isEqualTo(CallAnalysisStatus.COMPLETED);
        }

        @Test
        @DisplayName("callIds 가 비어있으면 빈 Map 을 반환한다")
        void findRequestedAnalysisSummariesByCallIds_whenEmptyCallIds_returnsEmpty() {
            // when
            final Map<Long, CallAnalysisSummary> result =
                    callAnalysisService.findRequestedAnalysisSummariesByCallIds(
                            USER_ID, List.of());

            // then
            assertThat(result).isEmpty();
        }
    }

    private Long saveEndedCall(final Long userA, final Long userB, final LocalDateTime endedAt) {
        final Call call = callRepository.save(
                Call.start(userA, userB, UUID.randomUUID(), endedAt.minusMinutes(5)));
        call.end(endedAt);
        return callRepository.save(call).getId();
    }

    private AnalysisResult twoMistakesResult() {
        return new AnalysisResult(
                new Mistakes(List.of(
                        new MistakeItem(FeedbackTag.GRAMMAR, "I goes", "I go", "1인칭 주어", "나는 간다"),
                        new MistakeItem(FeedbackTag.COLLOCATION, "make homework", "do homework", "결합", "숙제하다")
                )),
                Positives.empty()
        );
    }

    private AnalysisResult sampleResult() {
        return new AnalysisResult(
                new Mistakes(List.of(new MistakeItem(
                        FeedbackTag.GRAMMAR,
                        "I goes",
                        "I go",
                        "1인칭 주어",
                        "나는 간다"
                ))),
                new Positives(List.of(new PositiveItem(
                        "Sounds good.",
                        "자연스러운 동의 표현",
                        "좋아요."
                )))
        );
    }
}
