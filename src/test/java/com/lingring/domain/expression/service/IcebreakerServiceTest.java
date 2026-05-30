package com.lingring.domain.expression.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.lingring.domain.expression.dao.IcebreakerRepository;
import com.lingring.domain.expression.domain.Icebreaker;
import com.lingring.domain.expression.dto.response.IcebreakerListResponse;
import com.lingring.global.config.ServiceIntegrationHelper;
import com.lingring.global.error.ErrorCode;
import com.lingring.global.error.exception.NotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class IcebreakerServiceTest extends ServiceIntegrationHelper {

    @Autowired
    private IcebreakerService icebreakerService;

    @Autowired
    private IcebreakerRepository icebreakerRepository;

    private void saveIcebreakers(final int howMany) {
        for (int i = 0; i < howMany; i++) {
            icebreakerRepository.save(Icebreaker.create("expr" + i, "뜻" + i));
        }
    }

    @Nested
    @DisplayName("getRandom: 무작위 아이스브레이커 N개 조회")
    class GetRandom {

        @Test
        @DisplayName("DB가 비어있으면 ICEBREAKER_NOT_FOUND 예외가 발생한다")
        void getRandom_whenEmpty_throwsNotFound() {
            assertThatThrownBy(() -> icebreakerService.getRandom(5))
                    .isInstanceOf(NotFoundException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.ICEBREAKER_NOT_FOUND);
        }

        @Test
        @DisplayName("count만큼 아이스브레이커가 반환된다")
        void getRandom_returnsRequestedCount() {
            // given
            saveIcebreakers(10);

            // when
            final IcebreakerListResponse response = icebreakerService.getRandom(5);

            // then
            assertThat(response.items()).hasSize(5);
        }

        @Test
        @DisplayName("풀 크기보다 많이 요청해도 풀 크기만큼만 반환된다")
        void getRandom_capsByPoolSize() {
            // given
            saveIcebreakers(2);

            // when
            final IcebreakerListResponse response = icebreakerService.getRandom(10);

            // then
            assertThat(response.items()).hasSize(2);
        }

        @Test
        @DisplayName("count가 0 이하이면 PageSize 최소값(1)으로 clamp되어 1개를 반환한다")
        void getRandom_clampsCountBelowMin() {
            // given
            saveIcebreakers(3);

            // when
            final IcebreakerListResponse response = icebreakerService.getRandom(0);

            // then
            assertThat(response.items()).hasSize(1);
        }

        @Test
        @DisplayName("count가 50을 초과하면 PageSize 최대값(50)으로 clamp된다")
        void getRandom_clampsCountAboveMax() {
            // given
            saveIcebreakers(60);

            // when
            final IcebreakerListResponse response = icebreakerService.getRandom(100);

            // then
            assertThat(response.items()).hasSize(50);
        }

        @Test
        @DisplayName("응답 items에는 id/expression/meaning/createdAt이 채워져 있다")
        void getRandom_responseHasAllFields() {
            // given
            icebreakerRepository.save(Icebreaker.create("Hi!", "안녕!"));

            // when
            final IcebreakerListResponse response = icebreakerService.getRandom(1);

            // then
            assertThat(response.items()).hasSize(1);
            assertThat(response.items().getFirst().expression()).isEqualTo("Hi!");
            assertThat(response.items().getFirst().meaning()).isEqualTo("안녕!");
            assertThat(response.items().getFirst().id()).isNotNull();
            assertThat(response.items().getFirst().createdAt()).isNotNull();
        }
    }
}
