package com.lingring.domain.user.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.lingring.domain.user.dao.WithdrawalLogRepository;
import com.lingring.domain.user.domain.WithdrawReason;
import com.lingring.domain.user.domain.WithdrawalLog;
import com.lingring.global.config.ServiceIntegrationHelper;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class WithdrawalLogServiceTest extends ServiceIntegrationHelper {

    @Autowired
    private WithdrawalLogService withdrawalLogService;

    @Autowired
    private WithdrawalLogRepository withdrawalLogRepository;

    @Nested
    @DisplayName("record: 탈퇴 사유 기록")
    class Record {

        @Test
        @DisplayName("OTHER 사유 + description을 전달하면 row 1건이 저장되고 description이 보관된다")
        void record_whenOtherWithDescription_persistsRowWithDescription() {
            // given
            final String description = "더 이상 사용할 일이 없어요";

            // when
            withdrawalLogService.record(WithdrawReason.OTHER, description);

            // then
            final List<WithdrawalLog> all = withdrawalLogRepository.findAll();
            assertThat(all).hasSize(1);
            assertThat(all.get(0).getReason()).isEqualTo(WithdrawReason.OTHER);
            assertThat(all.get(0).getDescription().getValue()).isEqualTo(description);
        }

        @Test
        @DisplayName("OTHER가 아닌 사유는 description이 들어와도 row의 description이 null로 저장된다")
        void record_whenNonOtherWithDescription_persistsRowWithNullDescription() {
            // when
            withdrawalLogService.record(WithdrawReason.NO_GOOD_MATCH, "이건 저장 안 되어야 함");

            // then
            final List<WithdrawalLog> all = withdrawalLogRepository.findAll();
            assertThat(all).hasSize(1);
            assertThat(all.get(0).getReason()).isEqualTo(WithdrawReason.NO_GOOD_MATCH);
            assertThat(all.get(0).getDescription()).isNull();
        }

        @Test
        @DisplayName("createdAt이 자동으로 채워진다")
        void record_setsCreatedAtAutomatically() {
            // when
            withdrawalLogService.record(WithdrawReason.BUGGY, null);

            // then
            final WithdrawalLog saved = withdrawalLogRepository.findAll().get(0);
            assertThat(saved.getCreatedAt()).isNotNull();
        }
    }
}
