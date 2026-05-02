package com.lingring.domain.user.facade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.lingring.domain.auth.dao.RefreshTokenRepository;
import com.lingring.domain.call.dao.CallRepository;
import com.lingring.domain.call.domain.Call;
import com.lingring.domain.user.dao.UserRepository;
import com.lingring.domain.user.dao.UserStatsRepository;
import com.lingring.domain.user.domain.Provider;
import com.lingring.domain.user.domain.User;
import com.lingring.domain.user.domain.UserStats;
import com.lingring.domain.user.domain.WithdrawReason;
import com.lingring.domain.user.domain.vo.Name;
import com.lingring.domain.withdrawallog.dao.WithdrawalLogRepository;
import com.lingring.domain.withdrawallog.domain.WithdrawalLog;
import com.lingring.domain.userblock.dao.UserBlockRepository;
import com.lingring.domain.userblock.domain.UserBlock;
import com.lingring.domain.userexpression.dao.UserExpressionRepository;
import com.lingring.domain.userexpression.domain.UserExpression;
import com.lingring.domain.userreport.dao.UserReportRepository;
import com.lingring.domain.userreport.domain.ReportReason;
import com.lingring.domain.userreport.domain.UserReport;
import com.lingring.global.config.ServiceIntegrationHelper;
import com.lingring.global.error.ErrorCode;
import com.lingring.global.error.exception.NotFoundException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class UserWithdrawalFacadeTest extends ServiceIntegrationHelper {

    @Autowired
    private UserWithdrawalFacade userWithdrawalFacade;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserStatsRepository userStatsRepository;

    @Autowired
    private UserBlockRepository userBlockRepository;

    @Autowired
    private UserExpressionRepository userExpressionRepository;

    @Autowired
    private UserReportRepository userReportRepository;

    @Autowired
    private CallRepository callRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private WithdrawalLogRepository withdrawalLogRepository;

    @Nested
    @DisplayName("withdraw: 회원탈퇴 시 도메인별 정리 작업")
    class Withdraw {

        @Test
        @DisplayName("User row가 hard-delete된다")
        void withdraw_whenUserExists_deletesUserRow() {
            // given
            final User me = saveUser("링링", "kakao-me");

            // when
            userWithdrawalFacade.withdraw(me.getId(), WithdrawReason.NO_GOOD_MATCH, null);

            // then
            assertThat(userRepository.findById(me.getId())).isEmpty();
        }

        @Test
        @DisplayName("UserStats가 함께 삭제되고, 다른 사용자의 UserStats는 유지된다")
        void withdraw_whenUserStatsExist_cascadesDeleteOnlyMine() {
            // given
            final User me = saveUser("링링", "kakao-me");
            final User other = saveUser("타인", "kakao-other");
            userStatsRepository.save(UserStats.create(me.getId()));
            userStatsRepository.save(UserStats.create(other.getId()));

            // when
            userWithdrawalFacade.withdraw(me.getId(), WithdrawReason.NO_GOOD_MATCH, null);

            // then
            assertThat(userStatsRepository.findByUserId(me.getId())).isEmpty();
            assertThat(userStatsRepository.findByUserId(other.getId())).isPresent();
        }

        @Test
        @DisplayName("내 UserExpression은 삭제되고, 다른 사용자의 UserExpression은 유지된다")
        void withdraw_whenUserExpressionsExist_deletesOnlyMine() {
            // given
            final User me = saveUser("링링", "kakao-me");
            final User other = saveUser("타인", "kakao-other");
            userExpressionRepository.save(UserExpression.create(me.getId(), "expr-mine", "meaning-mine"));
            userExpressionRepository.save(UserExpression.create(me.getId(), "expr-mine-2", "meaning-mine-2"));
            userExpressionRepository.save(UserExpression.create(other.getId(), "expr-other", "meaning-other"));

            // when
            userWithdrawalFacade.withdraw(me.getId(), WithdrawReason.NO_GOOD_MATCH, null);

            // then
            assertThat(userExpressionRepository.findAllByUserIdOrderByCreatedAtDesc(
                    me.getId(), org.springframework.data.domain.PageRequest.of(0, 10)
            ).getContent()).isEmpty();
            assertThat(userExpressionRepository.findAllByUserIdOrderByCreatedAtDesc(
                    other.getId(), org.springframework.data.domain.PageRequest.of(0, 10)
            ).getContent()).hasSize(1);
        }

        @Test
        @DisplayName("내가 한 차단(UserBlock)은 삭제되지만, 내가 차단당한 차단은 유지된다")
        void withdraw_whenUserBlocksExist_deletesOnlyMyBlocks() {
            // given
            final User me = saveUser("링링", "kakao-me");
            final User a = saveUser("에이", "kakao-a");
            final User b = saveUser("비비", "kakao-b");
            userBlockRepository.save(UserBlock.create(me.getId(), a.getId()));
            userBlockRepository.save(UserBlock.create(b.getId(), me.getId()));

            // when
            userWithdrawalFacade.withdraw(me.getId(), WithdrawReason.NO_GOOD_MATCH, null);

            // then
            assertThat(userBlockRepository.findBlockedUserIdsByUserId(me.getId())).isEmpty();
            assertThat(userBlockRepository.findUserIdsByBlockedUserId(me.getId()))
                    .containsExactly(b.getId());
        }

        @Test
        @DisplayName("내가 참여한 Call의 내 위치만 NULL로 익명화되고, 상대방 ID는 그대로 유지된다")
        void withdraw_whenInvolvedInCalls_anonymizesOnlyMyId() {
            // given
            final User me = saveUser("링링", "kakao-me");
            final User partner1 = saveUser("파트너1", "kakao-p1");
            final User partner2 = saveUser("파트너2", "kakao-p2");
            final Call callA = callRepository.save(Call.start(
                    me.getId(), partner1.getId(), UUID.randomUUID(), LocalDateTime.now()
            ));
            final Call callB = callRepository.save(Call.start(
                    partner2.getId(), me.getId(), UUID.randomUUID(), LocalDateTime.now()
            ));
            final boolean meIsAInCallA = callA.getUserAId().equals(me.getId());
            final boolean meIsAInCallB = callB.getUserAId().equals(me.getId());

            // when
            userWithdrawalFacade.withdraw(me.getId(), WithdrawReason.NO_GOOD_MATCH, null);

            // then
            final Call reloadedA = callRepository.findById(callA.getId()).orElseThrow();
            final Call reloadedB = callRepository.findById(callB.getId()).orElseThrow();
            if (meIsAInCallA) {
                assertThat(reloadedA.getUserAId()).isNull();
                assertThat(reloadedA.getUserBId()).isEqualTo(partner1.getId());
            } else {
                assertThat(reloadedA.getUserBId()).isNull();
                assertThat(reloadedA.getUserAId()).isEqualTo(partner1.getId());
            }
            if (meIsAInCallB) {
                assertThat(reloadedB.getUserAId()).isNull();
                assertThat(reloadedB.getUserBId()).isEqualTo(partner2.getId());
            } else {
                assertThat(reloadedB.getUserBId()).isNull();
                assertThat(reloadedB.getUserAId()).isEqualTo(partner2.getId());
            }
        }

        @Test
        @DisplayName("내가 참여하지 않은 Call은 변경되지 않는다")
        void withdraw_whenNotInvolvedInCall_keepsCallUntouched() {
            // given
            final User me = saveUser("링링", "kakao-me");
            final User a = saveUser("에이", "kakao-a");
            final User b = saveUser("비비", "kakao-b");
            final Call call = callRepository.save(Call.start(
                    a.getId(), b.getId(), UUID.randomUUID(), LocalDateTime.now()
            ));

            // when
            userWithdrawalFacade.withdraw(me.getId(), WithdrawReason.NO_GOOD_MATCH, null);

            // then
            final Call reloaded = callRepository.findById(call.getId()).orElseThrow();
            assertThat(reloaded.getUserAId()).isEqualTo(call.getUserAId());
            assertThat(reloaded.getUserBId()).isEqualTo(call.getUserBId());
        }

        @Test
        @DisplayName("내가 신고자였던 UserReport의 userId만 NULL로 익명화되고, 신고 내용과 reportedUserId는 그대로 유지된다")
        void withdraw_whenIReported_anonymizesReporterIdOnly() {
            // given
            final User me = saveUser("링링", "kakao-me");
            final User reportedTarget = saveUser("타겟", "kakao-target");
            final UserReport saved = userReportRepository.save(UserReport.create(
                    me.getId(),
                    reportedTarget.getId(),
                    ReportReason.BAD_MANNERS,
                    "비매너 행동을 반복함"
            ));

            // when
            userWithdrawalFacade.withdraw(me.getId(), WithdrawReason.NO_GOOD_MATCH, null);

            // then
            final UserReport reloaded = userReportRepository.findById(saved.getId()).orElseThrow();
            assertThat(reloaded.getUserId()).isNull();
            assertThat(reloaded.getReportedUserId()).isEqualTo(reportedTarget.getId());
            assertThat(reloaded.getReason()).isEqualTo(ReportReason.BAD_MANNERS);
            assertThat(reloaded.getDescription().getValue()).isEqualTo("비매너 행동을 반복함");
        }

        @Test
        @DisplayName("내가 신고당한 UserReport는 그대로 유지된다 (reportedUserId 변경 없음)")
        void withdraw_whenIWasReported_keepsReportUntouched() {
            // given
            final User me = saveUser("링링", "kakao-me");
            final User reporter = saveUser("신고자", "kakao-reporter");
            final UserReport saved = userReportRepository.save(UserReport.create(
                    reporter.getId(),
                    me.getId(),
                    ReportReason.INAPPROPRIATE_CONVERSATION,
                    "부적절한 대화 시도"
            ));

            // when
            userWithdrawalFacade.withdraw(me.getId(), WithdrawReason.NO_GOOD_MATCH, null);

            // then
            final UserReport reloaded = userReportRepository.findById(saved.getId()).orElseThrow();
            assertThat(reloaded.getUserId()).isEqualTo(reporter.getId());
            assertThat(reloaded.getReportedUserId()).isEqualTo(me.getId());
        }

        @Test
        @DisplayName("RefreshToken이 폐기된다")
        void withdraw_whenRefreshTokenExists_deletesToken() {
            // given
            final User me = saveUser("링링", "kakao-me");
            refreshTokenRepository.save(me.getId(), "refresh-token-value");

            // when
            userWithdrawalFacade.withdraw(me.getId(), WithdrawReason.NO_GOOD_MATCH, null);

            // then
            assertThat(refreshTokenRepository.exists(me.getId())).isFalse();
        }

        @Test
        @DisplayName("존재하지 않는 userId로 호출하면 USER_NOT_FOUND 예외가 발생한다")
        void withdraw_whenUserNotFound_throwsNotFoundException() {
            // given
            final Long missingId = 9_999_999L;

            // when & then
            assertThatThrownBy(() -> userWithdrawalFacade.withdraw(missingId, WithdrawReason.NO_GOOD_MATCH, null))
                    .isInstanceOf(NotFoundException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.USER_NOT_FOUND);
        }

        @Test
        @DisplayName("OTHER 사유와 description을 같이 보내면 WithdrawalLog에 description이 저장된다")
        void withdraw_whenReasonIsOtherWithDescription_savesWithdrawalLogWithDescription() {
            // given
            final User me = saveUser("링링", "kakao-me");
            final String description = "더 이상 사용할 일이 없어요";

            // when
            userWithdrawalFacade.withdraw(me.getId(), WithdrawReason.OTHER, description);

            // then
            final List<WithdrawalLog> logs = withdrawalLogRepository.findAll();
            assertThat(logs).hasSize(1);
            assertThat(logs.get(0).getReason()).isEqualTo(WithdrawReason.OTHER);
            assertThat(logs.get(0).getDescription().getValue()).isEqualTo(description);
        }

        @Test
        @DisplayName("OTHER 아닌 사유로 호출하면 description이 들어와도 WithdrawalLog에는 description이 null로 저장된다")
        void withdraw_whenReasonIsNotOther_savesWithdrawalLogWithNullDescription() {
            // given
            final User me = saveUser("링링", "kakao-me");

            // when
            userWithdrawalFacade.withdraw(me.getId(), WithdrawReason.RARELY_USE, "이건 무시됨");

            // then
            final List<WithdrawalLog> logs = withdrawalLogRepository.findAll();
            assertThat(logs).hasSize(1);
            assertThat(logs.get(0).getReason()).isEqualTo(WithdrawReason.RARELY_USE);
            assertThat(logs.get(0).getDescription()).isNull();
        }
    }

    private User saveUser(final String nickname, final String providerSub) {
        return userRepository.save(
                User.createFromOAuth(Provider.KAKAO, providerSub, new Name(nickname), null)
        );
    }
}
