package com.lingring.domain.expression.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.lingring.domain.expression.dao.IcebreakerRepository;
import com.lingring.domain.expression.dao.UserExpressionRepository;
import com.lingring.domain.expression.domain.BookmarkSource;
import com.lingring.domain.expression.domain.Icebreaker;
import com.lingring.domain.expression.domain.UserExpression;
import com.lingring.domain.expression.dto.response.IcebreakerListResponse;
import com.lingring.domain.expression.dto.response.IcebreakerResponse;
import com.lingring.global.config.ServiceIntegrationHelper;
import com.lingring.global.error.ErrorCode;
import com.lingring.global.error.exception.NotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class IcebreakerServiceTest extends ServiceIntegrationHelper {

    private static final Long USER_ID = 1L;

    @Autowired
    private IcebreakerService icebreakerService;

    @Autowired
    private IcebreakerRepository icebreakerRepository;

    @Autowired
    private UserExpressionRepository userExpressionRepository;

    private void saveIcebreakers(final int howMany) {
        for (int i = 0; i < howMany; i++) {
            icebreakerRepository.save(Icebreaker.create("expr" + i, "뜻" + i));
        }
    }

    private UserExpression bookmarkIcebreaker(final Long userId, final Icebreaker icebreaker) {
        return userExpressionRepository.save(UserExpression.bookmark(
                userId,
                icebreaker.getExpression().getValue(),
                icebreaker.getMeaning().getValue(),
                BookmarkSource.ICEBREAKER,
                icebreaker.getId(),
                UserExpression.SHARED_SOURCE_SUB_INDEX
        ));
    }

    @Nested
    @DisplayName("getRandom: 무작위 아이스브레이커 N개 조회")
    class GetRandom {

        @Test
        @DisplayName("DB가 비어있으면 ICEBREAKER_NOT_FOUND 예외가 발생한다")
        void getRandom_whenEmpty_throwsNotFound() {
            assertThatThrownBy(() -> icebreakerService.getRandom(USER_ID, 5))
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
            final IcebreakerListResponse response = icebreakerService.getRandom(USER_ID, 5);

            // then
            assertThat(response.items()).hasSize(5);
        }

        @Test
        @DisplayName("풀 크기보다 많이 요청해도 풀 크기만큼만 반환된다")
        void getRandom_capsByPoolSize() {
            // given
            saveIcebreakers(2);

            // when
            final IcebreakerListResponse response = icebreakerService.getRandom(USER_ID, 10);

            // then
            assertThat(response.items()).hasSize(2);
        }

        @Test
        @DisplayName("count가 0 이하이면 PageSize 최소값(1)으로 clamp되어 1개를 반환한다")
        void getRandom_clampsCountBelowMin() {
            // given
            saveIcebreakers(3);

            // when
            final IcebreakerListResponse response = icebreakerService.getRandom(USER_ID, 0);

            // then
            assertThat(response.items()).hasSize(1);
        }

        @Test
        @DisplayName("count가 50을 초과하면 PageSize 최대값(50)으로 clamp된다")
        void getRandom_clampsCountAboveMax() {
            // given
            saveIcebreakers(60);

            // when
            final IcebreakerListResponse response = icebreakerService.getRandom(USER_ID, 100);

            // then
            assertThat(response.items()).hasSize(50);
        }

        @Test
        @DisplayName("응답 items에는 id/expression/meaning/createdAt이 채워져 있다")
        void getRandom_responseHasAllFields() {
            // given
            icebreakerRepository.save(Icebreaker.create("Hi!", "안녕!"));

            // when
            final IcebreakerListResponse response = icebreakerService.getRandom(USER_ID, 1);

            // then
            assertThat(response.items()).hasSize(1);
            assertThat(response.items().getFirst().expression()).isEqualTo("Hi!");
            assertThat(response.items().getFirst().meaning()).isEqualTo("안녕!");
            assertThat(response.items().getFirst().id()).isNotNull();
            assertThat(response.items().getFirst().createdAt()).isNotNull();
        }
    }

    @Nested
    @DisplayName("getRandom: 찜 상태(bookmarkId) 노출")
    class GetRandomBookmarkId {

        @Test
        @DisplayName("찜한 아이스브레이커는 bookmarkId가 채워지고, 안 찜한 것은 null이다")
        void getRandom_marksBookmarkedItemsOnly() {
            // given
            final Icebreaker bookmarked = icebreakerRepository.save(Icebreaker.create("A", "ㄱ"));
            final Icebreaker notBookmarked = icebreakerRepository.save(Icebreaker.create("B", "ㄴ"));
            final UserExpression bookmark = bookmarkIcebreaker(USER_ID, bookmarked);

            // when: 풀 전체(2개)를 받아 두 항목을 모두 확인
            final IcebreakerListResponse response = icebreakerService.getRandom(USER_ID, 2);

            // then
            final IcebreakerResponse bookmarkedItem = findItem(response, bookmarked.getId());
            final IcebreakerResponse plainItem = findItem(response, notBookmarked.getId());
            assertThat(bookmarkedItem.bookmarkId()).isEqualTo(bookmark.getId());
            assertThat(plainItem.bookmarkId()).isNull();
        }

        @Test
        @DisplayName("다른 사용자의 찜은 내 응답의 bookmarkId에 반영되지 않는다")
        void getRandom_doesNotLeakOtherUsersBookmarks() {
            // given
            final Long otherUserId = 2L;
            final Icebreaker icebreaker = icebreakerRepository.save(Icebreaker.create("A", "ㄱ"));
            bookmarkIcebreaker(otherUserId, icebreaker);

            // when
            final IcebreakerListResponse response = icebreakerService.getRandom(USER_ID, 1);

            // then
            assertThat(response.items().getFirst().bookmarkId()).isNull();
        }

        private IcebreakerResponse findItem(final IcebreakerListResponse response, final Long id) {
            return response.items().stream()
                    .filter(item -> item.id().equals(id))
                    .findFirst()
                    .orElseThrow();
        }
    }
}
