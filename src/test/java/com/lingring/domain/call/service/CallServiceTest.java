package com.lingring.domain.call.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.lingring.domain.call.dao.CallRepository;
import com.lingring.domain.call.domain.Call;
import com.lingring.domain.call.dto.response.CallsResponse;
import com.lingring.domain.call.dto.response.CallSummaryResponse;
import com.lingring.domain.call.event.CallEndedEvent;
import com.lingring.domain.call.exception.CallNotFoundException;
import com.lingring.domain.user.dao.UserRepository;
import com.lingring.domain.user.dao.UserStatsRepository;
import com.lingring.domain.user.domain.Provider;
import com.lingring.domain.user.domain.User;
import com.lingring.domain.user.domain.UserStats;
import com.lingring.domain.user.domain.vo.Name;
import com.lingring.global.config.ServiceIntegrationHelper;
import com.lingring.global.util.FixedDateTimeProvider;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;

@RecordApplicationEvents
@Import(CallServiceTest.FixedDateTimeProviderConfig.class)
class CallServiceTest extends ServiceIntegrationHelper {

    private static final LocalDateTime FIXED_NOW = LocalDateTime.of(2026, 5, 2, 10, 0);
    private static final LocalDate TODAY = FIXED_NOW.toLocalDate();

    @Autowired
    private CallService callService;

    @Autowired
    private CallRepository callRepository;

    @Autowired
    private UserStatsRepository userStatsRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private FixedDateTimeProvider fixedDateTimeProvider;

    @Autowired
    private ApplicationEvents events;

    @BeforeEach
    void resetClock() {
        fixedDateTimeProvider.setFixedTime(FIXED_NOW);
    }

    @TestConfiguration
    static class FixedDateTimeProviderConfig {

        @Bean
        @Primary
        FixedDateTimeProvider dateTimeProvider() {
            return new FixedDateTimeProvider(FIXED_NOW);
        }
    }

    @Nested
    @DisplayName("findByRoomId: 통화 조회 (선택적)")
    class FindByRoomId {

        @Test
        @DisplayName("roomId에 통화가 있으면 Optional에 담아 반환한다")
        void findByRoomId_whenPresent_returnsCall() {
            // given
            final UUID roomId = UUID.randomUUID();
            callRepository.save(Call.start(1L, 2L, roomId, FIXED_NOW.minusMinutes(5)));

            // when
            final Optional<Call> found = callService.findByRoomId(roomId);

            // then
            assertThat(found).isPresent();
            assertThat(found.get().getRoomId()).isEqualTo(roomId);
        }

        @Test
        @DisplayName("roomId에 통화가 없으면 빈 Optional을 반환한다")
        void findByRoomId_whenAbsent_returnsEmpty() {
            // when
            final Optional<Call> found = callService.findByRoomId(UUID.randomUUID());

            // then
            assertThat(found).isEmpty();
        }
    }

    @Nested
    @DisplayName("getByRoomId: 통화 조회 (필수)")
    class GetByRoomId {

        @Test
        @DisplayName("roomId에 통화가 있으면 Call을 반환한다")
        void getByRoomId_whenPresent_returnsCall() {
            // given
            final UUID roomId = UUID.randomUUID();
            callRepository.save(Call.start(1L, 2L, roomId, FIXED_NOW.minusMinutes(5)));

            // when
            final Call found = callService.getByRoomId(roomId);

            // then
            assertThat(found.getRoomId()).isEqualTo(roomId);
        }

        @Test
        @DisplayName("roomId에 통화가 없으면 CallNotFoundException을 던진다")
        void getByRoomId_whenAbsent_throws() {
            // when & then
            assertThatThrownBy(() -> callService.getByRoomId(UUID.randomUUID()))
                    .isInstanceOf(CallNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("endCall: 통화 종료")
    class EndCall {

        @Test
        @DisplayName("진행 중인 통화에 호출하면 isActive=false가 되고 endedAt이 기록된다")
        void endCall_marksCallEnded() {
            // given
            final UUID roomId = UUID.randomUUID();
            callRepository.save(Call.start(1L, 2L, roomId, FIXED_NOW.minusMinutes(5)));

            // when
            callService.endCall(roomId);

            // then
            final Optional<Call> ended = callRepository.findByRoomId(roomId);
            assertThat(ended).isPresent();
            assertThat(ended.get().isActive()).isFalse();
            assertThat(ended.get().getEndedAt()).isEqualTo(FIXED_NOW);
        }

        @Test
        @DisplayName("존재하지 않는 roomId면 CallNotFoundException을 던진다")
        void endCall_whenRoomIdNotFound_throws() {
            // given
            final UUID unknownRoomId = UUID.randomUUID();

            // when & then
            assertThatThrownBy(() -> callService.endCall(unknownRoomId))
                    .isInstanceOf(CallNotFoundException.class);
        }

        @Test
        @DisplayName("이미 종료된 통화에 다시 호출해도 endedAt이 변하지 않는다 (멱등)")
        void endCall_whenAlreadyEnded_isIdempotent() {
            // given
            final UUID roomId = UUID.randomUUID();
            callRepository.save(Call.start(1L, 2L, roomId, FIXED_NOW.minusMinutes(5)));
            callService.endCall(roomId);
            final LocalDateTime firstEndedAt = callRepository.findByRoomId(roomId)
                    .orElseThrow().getEndedAt();
            fixedDateTimeProvider.setFixedTime(FIXED_NOW.plusHours(1));

            // when
            callService.endCall(roomId);

            // then
            final Call ended = callRepository.findByRoomId(roomId).orElseThrow();
            assertThat(ended.isActive()).isFalse();
            assertThat(ended.getEndedAt()).isEqualTo(firstEndedAt);
        }
    }

    @Nested
    @DisplayName("endCall 이벤트 발행")
    class EndCallEventPublishing {

        @Test
        @DisplayName("진행 중인 통화 종료 시 CallEndedEvent를 한 번 발행한다 (페이로드: 두 사용자 ID, startedAt, endedAt)")
        void endCall_publishesEventWithPayload() {
            // given
            final UUID roomId = UUID.randomUUID();
            final LocalDateTime startedAt = FIXED_NOW.minusMinutes(5);
            callRepository.save(Call.start(1L, 2L, roomId, startedAt));

            // when
            callService.endCall(roomId);

            // then
            final List<CallEndedEvent> published = events.stream(CallEndedEvent.class).toList();
            assertThat(published).hasSize(1);
            assertThat(published.get(0).userAId()).isEqualTo(1L);
            assertThat(published.get(0).userBId()).isEqualTo(2L);
            assertThat(published.get(0).startedAt()).isEqualTo(startedAt);
            assertThat(published.get(0).endedAt()).isEqualTo(FIXED_NOW);
        }

        @Test
        @DisplayName("이미 종료된 통화에 호출하면 CallEndedEvent가 추가로 발행되지 않는다")
        void endCall_whenAlreadyEnded_doesNotRepublishEvent() {
            // given
            final UUID roomId = UUID.randomUUID();
            callRepository.save(Call.start(1L, 2L, roomId, FIXED_NOW.minusMinutes(5)));
            callService.endCall(roomId);

            // when
            callService.endCall(roomId);

            // then
            final List<CallEndedEvent> published = events.stream(CallEndedEvent.class).toList();
            assertThat(published).hasSize(1);
        }
    }

    @Nested
    @DisplayName("endCall + UserStats 갱신 통합")
    class EndCallUpdatesUserStats {

        @Test
        @DisplayName("통화 시간이 1분 이상이면 양쪽 사용자의 totalCallCount/currentStreakDays/lastStudyDate가 갱신된다")
        void endCall_whenDurationOverOneMinute_updatesBothUsersStats() {
            // given: 두 사용자의 stats 사전 저장
            userStatsRepository.save(UserStats.create(1L));
            userStatsRepository.save(UserStats.create(2L));
            final UUID roomId = UUID.randomUUID();
            callRepository.save(Call.start(1L, 2L, roomId, FIXED_NOW.minusMinutes(2)));

            // when
            callService.endCall(roomId);

            // then
            final UserStats statsA = userStatsRepository.findByUserId(1L).orElseThrow();
            final UserStats statsB = userStatsRepository.findByUserId(2L).orElseThrow();
            assertThat(statsA.getTotalCallCount()).isEqualTo(1);
            assertThat(statsA.getCurrentStreakDays()).isEqualTo(1);
            assertThat(statsA.getLastStudyDate()).isEqualTo(TODAY);
            assertThat(statsB.getTotalCallCount()).isEqualTo(1);
            assertThat(statsB.getCurrentStreakDays()).isEqualTo(1);
            assertThat(statsB.getLastStudyDate()).isEqualTo(TODAY);
        }

        @Test
        @DisplayName("통화 시간이 1분 미만이면 어느 사용자의 stats도 갱신되지 않는다")
        void endCall_whenDurationUnderOneMinute_doesNotUpdateStats() {
            // given
            userStatsRepository.save(UserStats.create(1L));
            userStatsRepository.save(UserStats.create(2L));
            final UUID roomId = UUID.randomUUID();
            callRepository.save(Call.start(1L, 2L, roomId, FIXED_NOW.minusSeconds(30)));

            // when
            callService.endCall(roomId);

            // then
            final UserStats statsA = userStatsRepository.findByUserId(1L).orElseThrow();
            final UserStats statsB = userStatsRepository.findByUserId(2L).orElseThrow();
            assertThat(statsA.getTotalCallCount()).isZero();
            assertThat(statsA.getLastStudyDate()).isNull();
            assertThat(statsB.getTotalCallCount()).isZero();
            assertThat(statsB.getLastStudyDate()).isNull();
        }

        @Test
        @DisplayName("통화 시간이 정확히 1분이면 stats가 갱신된다 (>= 60초 경계 포함)")
        void endCall_whenDurationExactlyOneMinute_updatesStats() {
            // given
            userStatsRepository.save(UserStats.create(1L));
            userStatsRepository.save(UserStats.create(2L));
            final UUID roomId = UUID.randomUUID();
            callRepository.save(Call.start(1L, 2L, roomId, FIXED_NOW.minusSeconds(60)));

            // when
            callService.endCall(roomId);

            // then
            assertThat(userStatsRepository.findByUserId(1L).orElseThrow().getTotalCallCount()).isEqualTo(1);
            assertThat(userStatsRepository.findByUserId(2L).orElseThrow().getTotalCallCount()).isEqualTo(1);
        }

        @Test
        @DisplayName("이미 종료된 통화에 다시 호출하면 stats가 추가로 갱신되지 않는다 (이벤트 미발행)")
        void endCall_whenAlreadyEnded_doesNotUpdateStatsAgain() {
            // given
            userStatsRepository.save(UserStats.create(1L));
            userStatsRepository.save(UserStats.create(2L));
            final UUID roomId = UUID.randomUUID();
            callRepository.save(Call.start(1L, 2L, roomId, FIXED_NOW.minusMinutes(2)));
            callService.endCall(roomId);

            // when
            callService.endCall(roomId);

            // then
            assertThat(userStatsRepository.findByUserId(1L).orElseThrow().getTotalCallCount()).isEqualTo(1);
            assertThat(userStatsRepository.findByUserId(2L).orElseThrow().getTotalCallCount()).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("getCallsByUserId: 내 통화 목록 조회")
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
            final CallsResponse response = callService.getCallsByUserId(me, 0, 20);

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
            final CallsResponse response = callService.getCallsByUserId(me, 0, 20);

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
            final CallsResponse response = callService.getCallsByUserId(me, 0, 20);

            // then
            assertThat(response.items()).isEmpty();
        }

        @Test
        @DisplayName("partner 정보(id, name)를 상대 사용자 기준으로 채운다 — 내가 userA여도, 내가 userB여도 동일")
        void getCallsByUserId_fillsPartnerCorrectly() {
            // given: 내 id가 더 작은 케이스(userA)와 더 큰 케이스(userB)를 둘 다 검증하기 위해 두 통화 생성
            final Long me = saveUser("me").getId();
            final Long smaller = saveUser("smaller").getId();   // me보다 작아질 수 있음
            final Long bigger = saveUser("bigger").getId();
            // start()가 자동으로 작은 id를 userA로 정렬하므로, partner는 항상 "다른 쪽"이 채워져야 함
            saveEndedCall(me, smaller, FIXED_NOW.minusHours(2), FIXED_NOW.minusHours(2).plusMinutes(1));
            saveEndedCall(me, bigger, FIXED_NOW.minusHours(1), FIXED_NOW.minusHours(1).plusMinutes(1));

            // when
            final CallsResponse response = callService.getCallsByUserId(me, 0, 20);

            // then
            assertThat(response.items()).hasSize(2);
            assertThat(response.items()).extracting(item -> item.partner().id())
                    .containsExactlyInAnyOrder(smaller, bigger);
            assertThat(response.items()).extracting(item -> item.partner().name())
                    .containsExactlyInAnyOrder("smaller", "bigger");
        }

        @Test
        @DisplayName("durationSec은 endedAt - startedAt 초로 계산된다")
        void getCallsByUserId_computesDurationSec() {
            // given
            final Long me = saveUser("유저").getId();
            final Long partner = saveUser("Sophie").getId();
            saveEndedCall(me, partner, FIXED_NOW.minusMinutes(10), FIXED_NOW.minusMinutes(10).plusSeconds(312));

            // when
            final CallsResponse response = callService.getCallsByUserId(me, 0, 20);

            // then
            assertThat(response.items().get(0).durationSec()).isEqualTo(312);
        }

        @Test
        @DisplayName("size를 초과한 경우 hasNext=true, 다음 페이지가 존재함을 표시한다")
        void getCallsByUserId_returnsHasNextWhenMoreExist() {
            // given: size=2, 통화는 3개
            final Long me = saveUser("유저").getId();
            final Long partner = saveUser("Sophie").getId();
            for (int i = 1; i <= 3; i++) {
                saveEndedCall(me, partner, FIXED_NOW.minusMinutes(i * 10L), FIXED_NOW.minusMinutes(i * 10L).plusMinutes(1));
            }

            // when
            final CallsResponse response = callService.getCallsByUserId(me, 0, 2);

            // then
            assertThat(response.items()).hasSize(2);
            assertThat(response.hasNext()).isTrue();
        }

        @Test
        @DisplayName("size가 50을 초과하면 50으로 clamp된다")
        void getCallsByUserId_clampsSizeToMax() {
            // given: 51개 통화, size=100 요청
            final Long me = saveUser("유저").getId();
            final Long partner = saveUser("Sophie").getId();
            for (int i = 1; i <= 51; i++) {
                saveEndedCall(me, partner, FIXED_NOW.minusMinutes(i * 10L), FIXED_NOW.minusMinutes(i * 10L).plusMinutes(1));
            }

            // when
            final CallsResponse response = callService.getCallsByUserId(me, 0, 100);

            // then: 50개로 잘림 (clamp), 다음 페이지 존재
            assertThat(response.items()).hasSize(50);
            assertThat(response.hasNext()).isTrue();
        }

        private User saveUser(final String name) {
            return userRepository.save(
                    User.createFromOAuth(Provider.KAKAO, "sub-" + name + "-" + UUID.randomUUID(), new Name(name), null)
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
}
