package com.lingring.domain.call.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.lingring.domain.call.dao.CallRepository;
import com.lingring.domain.call.domain.Call;
import com.lingring.domain.call.dto.response.CallSummaryResponse;
import com.lingring.domain.call.dto.response.CallsResponse;
import com.lingring.domain.review.domain.analysis.CallAnalysis;
import com.lingring.domain.review.domain.analysis.vo.AnalysisResult;
import com.lingring.domain.review.domain.analysis.vo.FeedbackTag;
import com.lingring.domain.review.domain.analysis.vo.MistakeItem;
import com.lingring.domain.review.domain.analysis.vo.Mistakes;
import com.lingring.domain.review.domain.analysis.vo.PositiveItem;
import com.lingring.domain.review.domain.analysis.vo.Positives;
import com.lingring.domain.call.dto.response.CallAnalysisStatusView;
import com.lingring.domain.review.dao.CallRecordingRepository;
import com.lingring.domain.review.domain.recording.CallRecording;
import com.lingring.domain.review.service.CallAnalysisService;
import com.lingring.domain.user.dao.UserRepository;
import com.lingring.domain.user.domain.Provider;
import com.lingring.domain.user.domain.User;
import com.lingring.domain.user.domain.vo.Name;
import com.lingring.global.config.ServiceIntegrationHelper;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

class CallHistoryServiceTest extends ServiceIntegrationHelper {

    private static final LocalDateTime FIXED_NOW = LocalDateTime.of(2026, 5, 2, 10, 0);

    @Autowired
    private CallHistoryService callHistoryService;

    @Autowired
    private CallAnalysisService callAnalysisService;

    @Autowired
    private CallRecordingRepository callRecordingRepository;

    @Autowired
    private CallRepository callRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    private static final String MODEL = "gemini-2.5-flash";

    @Nested
    @DisplayName("getCallsByUserId: 통화 목록 (analysis enrichment 포함)")
    class GetCallsByUserId {

        @Test
        @DisplayName("종료된 통화만 반환하고 진행 중인 통화는 제외한다")
        void getCallsByUserId_excludesActiveCalls() {
            // given
            final Long me = saveUser("유저").getId();
            final Long partner = saveUser("Sophie").getId();
            final Call ended = saveEndedCall(me, partner, FIXED_NOW.minusMinutes(10), FIXED_NOW.minusMinutes(5));
            callRepository.save(Call.start(me, partner, UUID.randomUUID(), FIXED_NOW.minusMinutes(2))); // active

            // when
            final CallsResponse response = callHistoryService.getCallsByUserId(me, 0, 20);

            // then
            assertThat(response.items()).hasSize(1);
            assertThat(response.items().get(0).id()).isEqualTo(ended.getId());
        }

        @Test
        @DisplayName("startedAt 내림차순으로 정렬된다")
        void getCallsByUserId_orderByStartedAtDesc() {
            // given
            final Long me = saveUser("유저").getId();
            final Long partner = saveUser("Sophie").getId();
            final Call older = saveEndedCall(me, partner, FIXED_NOW.minusHours(2), FIXED_NOW.minusHours(2).plusMinutes(5));
            final Call newer = saveEndedCall(me, partner, FIXED_NOW.minusMinutes(30), FIXED_NOW.minusMinutes(25));

            // when
            final CallsResponse response = callHistoryService.getCallsByUserId(me, 0, 20);

            // then
            assertThat(response.items()).extracting(CallSummaryResponse::id)
                    .containsExactly(newer.getId(), older.getId());
        }

        @Test
        @DisplayName("내가 참여하지 않은 통화는 반환하지 않는다")
        void getCallsByUserId_excludesCallsImNotIn() {
            // given
            final Long me = saveUser("유저").getId();
            final Long u2 = saveUser("user2").getId();
            final Long u3 = saveUser("user3").getId();
            saveEndedCall(u2, u3, FIXED_NOW.minusHours(1), FIXED_NOW.minusMinutes(50));

            // when
            final CallsResponse response = callHistoryService.getCallsByUserId(me, 0, 20);

            // then
            assertThat(response.items()).isEmpty();
        }

        @Test
        @DisplayName("partner 정보(id, name, profileImage)를 상대 사용자 기준으로 채운다")
        void getCallsByUserId_fillsPartnerCorrectly() {
            // given
            final Long me = saveUser("me").getId();
            final Long withImage = saveUser("withImage", "https://cdn.example.com/p/with-image.png").getId();
            final Long noImage = saveUser("noImage").getId();
            saveEndedCall(me, withImage, FIXED_NOW.minusHours(2), FIXED_NOW.minusHours(2).plusMinutes(1));
            saveEndedCall(me, noImage, FIXED_NOW.minusHours(1), FIXED_NOW.minusHours(1).plusMinutes(1));

            // when
            final CallsResponse response = callHistoryService.getCallsByUserId(me, 0, 20);

            // then
            assertThat(response.items()).hasSize(2);
            assertThat(response.items()).extracting(item -> item.partner().id())
                    .containsExactlyInAnyOrder(withImage, noImage);
            assertThat(response.items()).extracting(item -> item.partner().name())
                    .containsExactlyInAnyOrder("withImage", "noImage");
            assertThat(response.items()).extracting(item -> item.partner().profileImage())
                    .containsExactlyInAnyOrder("https://cdn.example.com/p/with-image.png", null);
        }

        @Test
        @DisplayName("durationSec은 endedAt - startedAt 초로 계산된다")
        void getCallsByUserId_computesDurationSec() {
            // given
            final Long me = saveUser("유저").getId();
            final Long partner = saveUser("Sophie").getId();
            saveEndedCall(me, partner, FIXED_NOW.minusMinutes(10), FIXED_NOW.minusMinutes(10).plusSeconds(312));

            // when
            final CallsResponse response = callHistoryService.getCallsByUserId(me, 0, 20);

            // then
            assertThat(response.items().get(0).durationSec()).isEqualTo(312);
        }

        @Test
        @DisplayName("size를 초과한 경우 hasNext=true")
        void getCallsByUserId_returnsHasNextWhenMoreExist() {
            // given
            final Long me = saveUser("유저").getId();
            final Long partner = saveUser("Sophie").getId();
            for (int i = 1; i <= 3; i++) {
                saveEndedCall(me, partner, FIXED_NOW.minusMinutes(i * 10L), FIXED_NOW.minusMinutes(i * 10L).plusMinutes(1));
            }

            // when
            final CallsResponse response = callHistoryService.getCallsByUserId(me, 0, 2);

            // then
            assertThat(response.items()).hasSize(2);
            assertThat(response.hasNext()).isTrue();
        }

        @Test
        @DisplayName("size가 50을 초과하면 50으로 clamp된다")
        void getCallsByUserId_clampsSizeToMax() {
            // given
            final Long me = saveUser("유저").getId();
            final Long partner = saveUser("Sophie").getId();
            for (int i = 1; i <= 51; i++) {
                saveEndedCall(me, partner, FIXED_NOW.minusMinutes(i * 10L), FIXED_NOW.minusMinutes(i * 10L).plusMinutes(1));
            }

            // when
            final CallsResponse response = callHistoryService.getCallsByUserId(me, 0, 100);

            // then
            assertThat(response.items()).hasSize(50);
            assertThat(response.hasNext()).isTrue();
        }

        @Test
        @DisplayName("1분 미만 통화도 목록에 포함된다")
        void getCallsByUserId_includesCallsUnderOneMinute() {
            // given
            final Long me = saveUser("유저").getId();
            final Long partner = saveUser("Sophie").getId();
            final Call under = saveEndedCall(me, partner, FIXED_NOW.minusMinutes(10), FIXED_NOW.minusMinutes(10).plusSeconds(30));
            final Call over = saveEndedCall(me, partner, FIXED_NOW.minusMinutes(5), FIXED_NOW.minusMinutes(5).plusMinutes(2));

            // when
            final CallsResponse response = callHistoryService.getCallsByUserId(me, 0, 20);

            // then
            assertThat(response.items()).extracting(CallSummaryResponse::id)
                    .containsExactly(over.getId(), under.getId());
        }

        @Test
        @DisplayName("상대방이 탈퇴해도 통화 기록은 노출되며 partner는 null로 응답한다")
        void getCallsByUserId_includesCallsWithWithdrawnPartner() {
            // given
            final Long me = saveUser("me").getId();
            final User partner = saveUser("탈퇴자");
            final Call ended = saveEndedCall(me, partner.getId(), FIXED_NOW.minusMinutes(10), FIXED_NOW.minusMinutes(5));

            // when
            new TransactionTemplate(transactionManager).executeWithoutResult(status ->
                    callRepository.anonymizeUser(partner.getId()));
            userRepository.deleteById(partner.getId());

            final CallsResponse response = callHistoryService.getCallsByUserId(me, 0, 20);

            // then
            assertThat(response.items()).hasSize(1);
            assertThat(response.items().get(0).id()).isEqualTo(ended.getId());
            assertThat(response.items().get(0).partner()).isNull();
        }
    }

    @Nested
    @DisplayName("analysis enrichment (analysisId + analysisStatus)")
    class AnalysisEnrichment {

        @Test
        @DisplayName("분석 요청 안 했고 두 녹음이 모두 올라왔으면 analysisId=null, analysisStatus=READY")
        void enrichment_whenNotRequestedAndRecordingsReady_returnsReady() {
            // given
            final Long me = saveUser("me").getId();
            final Long partner = saveUser("Sophie").getId();
            final Call call = saveEndedCall(me, partner, FIXED_NOW.minusMinutes(10), FIXED_NOW.minusMinutes(5));
            saveBothRecordings(call.getId(), me, partner);

            // when
            final CallsResponse response = callHistoryService.getCallsByUserId(me, 0, 20);

            // then
            assertThat(response.items()).hasSize(1);
            assertThat(response.items().get(0).analysisId()).isNull();
            assertThat(response.items().get(0).analysisStatus()).isEqualTo(CallAnalysisStatusView.READY);
        }

        @Test
        @DisplayName("분석 요청 안 했고 녹음이 아직 다 안 올라온 통화는 analysisStatus=WAITING_RECORDINGS (버튼 비활성)")
        void enrichment_whenNotRequestedAndRecordingsNotReady_returnsWaitingRecordings() {
            // given: 한쪽 녹음만 올라온 상태 (아직 준비 안 됨)
            final Long me = saveUser("me").getId();
            final Long partner = saveUser("Sophie").getId();
            final Call call = saveEndedCall(me, partner, FIXED_NOW.minusMinutes(10), FIXED_NOW.minusMinutes(5));
            saveRecording(call.getId(), me);

            // when
            final CallsResponse response = callHistoryService.getCallsByUserId(me, 0, 20);

            // then
            assertThat(response.items()).hasSize(1);
            assertThat(response.items().get(0).analysisId()).isNull();
            assertThat(response.items().get(0).analysisStatus())
                    .isEqualTo(CallAnalysisStatusView.WAITING_RECORDINGS);
        }

        @Test
        @DisplayName("내가 직접 requestForUser 한 직후 통화는 analysisId 채워지고 analysisStatus=PROCESSING")
        void enrichment_whenSelfRequested_returnsProcessing() {
            // given
            final Long me = saveUser("me").getId();
            final Long partner = saveUser("Sophie").getId();
            final Call call = saveEndedCall(me, partner, FIXED_NOW.minusMinutes(10), FIXED_NOW.minusMinutes(5));
            final CallAnalysis mine = callAnalysisService.requestForUser(call.getId(), me);

            // when
            final CallsResponse response = callHistoryService.getCallsByUserId(me, 0, 20);

            // then
            assertThat(response.items()).hasSize(1);
            assertThat(response.items().get(0).analysisId()).isEqualTo(mine.getId());
            assertThat(response.items().get(0).analysisStatus()).isEqualTo(CallAnalysisStatusView.PROCESSING);
        }

        @Test
        @DisplayName("본인 분석이 완료되면 analysisStatus=COMPLETED")
        void enrichment_whenCompleted_returnsCompleted() {
            // given
            final Long me = saveUser("me").getId();
            final Long partner = saveUser("Sophie").getId();
            final Call call = saveEndedCall(me, partner, FIXED_NOW.minusMinutes(10), FIXED_NOW.minusMinutes(5));
            final CallAnalysis mine = callAnalysisService.requestForUser(call.getId(), me);
            callAnalysisService.complete(call.getId(), me, sampleResult(), MODEL);

            // when
            final CallsResponse response = callHistoryService.getCallsByUserId(me, 0, 20);

            // then
            assertThat(response.items()).hasSize(1);
            assertThat(response.items().get(0).analysisId()).isEqualTo(mine.getId());
            assertThat(response.items().get(0).analysisStatus()).isEqualTo(CallAnalysisStatusView.COMPLETED);
        }

        @Test
        @DisplayName("상대만 requestForUser 한 통화는 본인 시점에 analysisStatus=READY (분석이 실제로 완료됐어도 격리됨)")
        void enrichment_whenOnlyPeerRequestedAndCompleted_returnsReadyForSelf() {
            // given: 상대만 트리거, 분석이 양쪽 다 완료된 상황
            final Long me = saveUser("me").getId();
            final Long partner = saveUser("Sophie").getId();
            final Call call = saveEndedCall(me, partner, FIXED_NOW.minusMinutes(10), FIXED_NOW.minusMinutes(5));
            saveBothRecordings(call.getId(), me, partner);
            callAnalysisService.requestForUser(call.getId(), partner);
            callAnalysisService.ensureExistsForUser(call.getId(), me);
            callAnalysisService.complete(call.getId(), partner, sampleResult(), MODEL);
            callAnalysisService.complete(call.getId(), me, sampleResult(), MODEL);

            // when
            final CallsResponse response = callHistoryService.getCallsByUserId(me, 0, 20);

            // then: 본인이 요청 안 했으므로 READY (녹음은 준비된 상태)
            assertThat(response.items().get(0).analysisId()).isNull();
            assertThat(response.items().get(0).analysisStatus()).isEqualTo(CallAnalysisStatusView.READY);
        }

        @Test
        @DisplayName("여러 통화가 섞여 있을 때 각 통화의 본인 analysisId/Status 매핑이 올바르다")
        void enrichment_acrossMultipleCalls_mapsCorrectly() {
            // given
            final Long me = saveUser("me").getId();
            final Long partner = saveUser("Sophie").getId();
            final Call c1 = saveEndedCall(me, partner, FIXED_NOW.minusHours(3), FIXED_NOW.minusHours(3).plusMinutes(2));
            final Call c2 = saveEndedCall(me, partner, FIXED_NOW.minusHours(2), FIXED_NOW.minusHours(2).plusMinutes(2));
            final Call c3 = saveEndedCall(me, partner, FIXED_NOW.minusHours(1), FIXED_NOW.minusHours(1).plusMinutes(2));
            saveBothRecordings(c1.getId(), me, partner);
            saveBothRecordings(c2.getId(), me, partner);
            saveBothRecordings(c3.getId(), me, partner);
            final CallAnalysis a1 = callAnalysisService.requestForUser(c1.getId(), me);
            callAnalysisService.complete(c1.getId(), me, sampleResult(), MODEL);
            // c2 — 본인 요청 안 함
            callAnalysisService.ensureExistsForUser(c2.getId(), me);
            final CallAnalysis a3 = callAnalysisService.requestForUser(c3.getId(), me);

            // when
            final CallsResponse response = callHistoryService.getCallsByUserId(me, 0, 20);

            // then: 정렬은 startedAt DESC 라 c3, c2, c1 순
            assertThat(response.items()).hasSize(3);
            assertThat(response.items().get(0).id()).isEqualTo(c3.getId());
            assertThat(response.items().get(0).analysisId()).isEqualTo(a3.getId());
            assertThat(response.items().get(0).analysisStatus()).isEqualTo(CallAnalysisStatusView.PROCESSING);
            assertThat(response.items().get(1).id()).isEqualTo(c2.getId());
            assertThat(response.items().get(1).analysisId()).isNull();
            assertThat(response.items().get(1).analysisStatus()).isEqualTo(CallAnalysisStatusView.READY);
            assertThat(response.items().get(2).id()).isEqualTo(c1.getId());
            assertThat(response.items().get(2).analysisId()).isEqualTo(a1.getId());
            assertThat(response.items().get(2).analysisStatus()).isEqualTo(CallAnalysisStatusView.COMPLETED);
        }
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
                        "Nice greeting",
                        "Hello",
                        "안녕"
                )))
        );
    }

    private User saveUser(final String name) {
        return saveUser(name, null);
    }

    private User saveUser(final String name, final String profileImageUrl) {
        return userRepository.save(
                User.createFromOAuth(Provider.KAKAO, "sub-" + name + "-" + UUID.randomUUID(), new Name(name), profileImageUrl)
        );
    }

    private Call saveEndedCall(
            final Long userA,
            final Long userB,
            final LocalDateTime startedAt,
            final LocalDateTime endedAt
    ) {
        final Call call = callRepository.save(Call.start(userA, userB, UUID.randomUUID(), startedAt));
        call.end(endedAt);
        return callRepository.save(call);
    }

    private void saveBothRecordings(final Long callId, final Long userA, final Long userB) {
        saveRecording(callId, userA);
        saveRecording(callId, userB);
    }

    private void saveRecording(final Long callId, final Long userId) {
        callRecordingRepository.save(CallRecording.upload(
                callId, userId, "call-recordings/%d/%d/key".formatted(callId, userId), "audio/m4a"));
    }
}
