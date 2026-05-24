package com.lingring.domain.call.facade;

import static org.assertj.core.api.Assertions.assertThat;

import com.lingring.domain.call.dao.CallRepository;
import com.lingring.domain.call.domain.Call;
import com.lingring.domain.call.dto.response.CallSummaryResponse;
import com.lingring.domain.call.dto.response.CallsResponse;
import com.lingring.domain.call.service.CallService;
import com.lingring.domain.callanalysis.domain.CallAnalysis;
import com.lingring.domain.callanalysis.service.CallAnalysisService;
import com.lingring.domain.user.dao.UserRepository;
import com.lingring.domain.user.domain.Provider;
import com.lingring.domain.user.domain.User;
import com.lingring.domain.user.domain.vo.Name;
import com.lingring.global.config.ServiceIntegrationHelper;
import java.time.LocalDateTime;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class CallHistoryFacadeTest extends ServiceIntegrationHelper {

    private static final LocalDateTime FIXED_NOW = LocalDateTime.of(2026, 5, 2, 10, 0);

    @Autowired
    private CallHistoryFacade callHistoryFacade;

    @Autowired
    private CallService callService;

    @Autowired
    private CallAnalysisService callAnalysisService;

    @Autowired
    private CallRepository callRepository;

    @Autowired
    private UserRepository userRepository;

    @Nested
    @DisplayName("getCallsByUserId: 통화 목록 (analysisId enrichment 포함)")
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
            final CallsResponse response = callHistoryFacade.getCallsByUserId(me, 0, 20);

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
            final CallsResponse response = callHistoryFacade.getCallsByUserId(me, 0, 20);

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
            final CallsResponse response = callHistoryFacade.getCallsByUserId(me, 0, 20);

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
            final CallsResponse response = callHistoryFacade.getCallsByUserId(me, 0, 20);

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
            final CallsResponse response = callHistoryFacade.getCallsByUserId(me, 0, 20);

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
            final CallsResponse response = callHistoryFacade.getCallsByUserId(me, 0, 2);

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
            final CallsResponse response = callHistoryFacade.getCallsByUserId(me, 0, 100);

            // then
            assertThat(response.items()).hasSize(50);
            assertThat(response.hasNext()).isTrue();
        }

        @Test
        @DisplayName("1분 미만 통화는 목록에서 제외된다")
        void getCallsByUserId_excludesCallsUnderOneMinute() {
            // given
            final Long me = saveUser("유저").getId();
            final Long partner = saveUser("Sophie").getId();
            saveEndedCall(me, partner, FIXED_NOW.minusMinutes(10), FIXED_NOW.minusMinutes(10).plusSeconds(30));
            final Call longEnough = saveEndedCall(me, partner, FIXED_NOW.minusMinutes(5), FIXED_NOW.minusMinutes(5).plusMinutes(2));

            // when
            final CallsResponse response = callHistoryFacade.getCallsByUserId(me, 0, 20);

            // then
            assertThat(response.items()).hasSize(1);
            assertThat(response.items().get(0).id()).isEqualTo(longEnough.getId());
        }

        @Test
        @DisplayName("상대방이 탈퇴해도 통화 기록은 노출되며 partner는 null로 응답한다")
        void getCallsByUserId_includesCallsWithWithdrawnPartner() {
            // given
            final Long me = saveUser("me").getId();
            final User partner = saveUser("탈퇴자");
            final Call ended = saveEndedCall(me, partner.getId(), FIXED_NOW.minusMinutes(10), FIXED_NOW.minusMinutes(5));

            // when
            callService.anonymizeUser(partner.getId());
            userRepository.deleteById(partner.getId());

            final CallsResponse response = callHistoryFacade.getCallsByUserId(me, 0, 20);

            // then
            assertThat(response.items()).hasSize(1);
            assertThat(response.items().get(0).id()).isEqualTo(ended.getId());
            assertThat(response.items().get(0).partner()).isNull();
        }
    }

    @Nested
    @DisplayName("analysisId enrichment")
    class AnalysisIdEnrichment {

        @Test
        @DisplayName("분석 요청 안 한 통화는 analysisId가 null")
        void enrichment_whenNotRequested_returnsNull() {
            // given
            final Long me = saveUser("me").getId();
            final Long partner = saveUser("Sophie").getId();
            saveEndedCall(me, partner, FIXED_NOW.minusMinutes(10), FIXED_NOW.minusMinutes(5));

            // when
            final CallsResponse response = callHistoryFacade.getCallsByUserId(me, 0, 20);

            // then
            assertThat(response.items()).hasSize(1);
            assertThat(response.items().get(0).analysisId()).isNull();
        }

        @Test
        @DisplayName("내가 직접 requestForUser 한 통화는 본인 analysisId가 채워진다")
        void enrichment_whenSelfRequested_returnsAnalysisId() {
            // given
            final Long me = saveUser("me").getId();
            final Long partner = saveUser("Sophie").getId();
            final Call call = saveEndedCall(me, partner, FIXED_NOW.minusMinutes(10), FIXED_NOW.minusMinutes(5));
            final CallAnalysis mine = callAnalysisService.requestForUser(call.getId(), me);

            // when
            final CallsResponse response = callHistoryFacade.getCallsByUserId(me, 0, 20);

            // then
            assertThat(response.items()).hasSize(1);
            assertThat(response.items().get(0).analysisId()).isEqualTo(mine.getId());
        }

        @Test
        @DisplayName("상대만 requestForUser 했고 본인은 ensureExistsForUser만 된 통화는 analysisId가 null")
        void enrichment_whenOnlyPeerRequested_returnsNullForSelf() {
            // given: 상대가 트리거. 본인 행은 placeholder(requested=false)로 생성됨
            final Long me = saveUser("me").getId();
            final Long partner = saveUser("Sophie").getId();
            final Call call = saveEndedCall(me, partner, FIXED_NOW.minusMinutes(10), FIXED_NOW.minusMinutes(5));
            callAnalysisService.requestForUser(call.getId(), partner);
            callAnalysisService.ensureExistsForUser(call.getId(), me);

            // when
            final CallsResponse response = callHistoryFacade.getCallsByUserId(me, 0, 20);

            // then
            assertThat(response.items().get(0).analysisId()).isNull();
        }

        @Test
        @DisplayName("여러 통화가 섞여 있을 때 각 통화의 본인 analysisId 매핑이 올바르다 (N+1 없이 한 쿼리로 enrichment)")
        void enrichment_acrossMultipleCalls_mapsCorrectly() {
            // given
            final Long me = saveUser("me").getId();
            final Long partner = saveUser("Sophie").getId();
            final Call c1 = saveEndedCall(me, partner, FIXED_NOW.minusHours(3), FIXED_NOW.minusHours(3).plusMinutes(2));
            final Call c2 = saveEndedCall(me, partner, FIXED_NOW.minusHours(2), FIXED_NOW.minusHours(2).plusMinutes(2));
            final Call c3 = saveEndedCall(me, partner, FIXED_NOW.minusHours(1), FIXED_NOW.minusHours(1).plusMinutes(2));
            final CallAnalysis a1 = callAnalysisService.requestForUser(c1.getId(), me);
            // c2 — 본인 요청 안 함
            callAnalysisService.ensureExistsForUser(c2.getId(), me);
            final CallAnalysis a3 = callAnalysisService.requestForUser(c3.getId(), me);

            // when
            final CallsResponse response = callHistoryFacade.getCallsByUserId(me, 0, 20);

            // then: 정렬은 startedAt DESC 라 c3, c2, c1 순
            assertThat(response.items()).hasSize(3);
            assertThat(response.items().get(0).id()).isEqualTo(c3.getId());
            assertThat(response.items().get(0).analysisId()).isEqualTo(a3.getId());
            assertThat(response.items().get(1).id()).isEqualTo(c2.getId());
            assertThat(response.items().get(1).analysisId()).isNull();
            assertThat(response.items().get(2).id()).isEqualTo(c1.getId());
            assertThat(response.items().get(2).analysisId()).isEqualTo(a1.getId());
        }
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
}
