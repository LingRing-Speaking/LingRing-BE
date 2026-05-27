package com.lingring.domain.moderation.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.lingring.domain.user.dao.UserRepository;
import com.lingring.domain.user.domain.Provider;
import com.lingring.domain.user.domain.User;
import com.lingring.domain.user.domain.vo.Name;
import com.lingring.domain.moderation.dao.UserBlockRepository;
import com.lingring.domain.moderation.domain.UserBlock;
import com.lingring.domain.moderation.dto.request.UserBlockCreateRequest;
import com.lingring.domain.moderation.dto.response.UserBlockItemResponse;
import com.lingring.domain.moderation.dto.response.UserBlockResponse;
import com.lingring.domain.moderation.dto.response.UserBlocksResponse;
import com.lingring.global.config.ServiceIntegrationHelper;
import com.lingring.global.error.ErrorCode;
import com.lingring.global.error.exception.BadRequestException;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class UserBlockServiceTest extends ServiceIntegrationHelper {

    @Autowired
    private UserBlockService userBlockService;

    @Autowired
    private UserBlockRepository userBlockRepository;

    @Autowired
    private UserRepository userRepository;

    @Nested
    @DisplayName("block: 사용자 차단")
    class Block {

        @Test
        @DisplayName("유효한 요청이면 차단 row가 생성되고 응답을 반환한다")
        void block_whenValid_returnsResponse() {
            // given
            final Long userId = 1L;
            final UserBlockCreateRequest request = new UserBlockCreateRequest(2L);

            // when
            final UserBlockResponse response = userBlockService.block(userId, request);

            // then
            assertThat(response.id()).isNotNull();
            assertThat(response.userId()).isEqualTo(userId);
            assertThat(response.blockedUserId()).isEqualTo(2L);
            assertThat(userBlockRepository.findById(response.id())).isPresent();
        }

        @Test
        @DisplayName("자기 자신을 차단하면 SELF_BLOCK_NOT_ALLOWED 예외가 발생한다")
        void block_whenSelfBlock_throwsBadRequest() {
            // given
            final Long userId = 1L;
            final UserBlockCreateRequest request = new UserBlockCreateRequest(userId);

            // when & then
            assertThatThrownBy(() -> userBlockService.block(userId, request))
                    .isInstanceOf(BadRequestException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.SELF_BLOCK_NOT_ALLOWED);
        }

        @Test
        @DisplayName("이미 차단된 대상을 다시 차단하면 기존 row의 id를 반환한다 (멱등)")
        void block_whenAlreadyBlocked_returnsExistingRow() {
            // given
            final Long userId = 1L;
            final UserBlockCreateRequest request = new UserBlockCreateRequest(2L);
            final UserBlockResponse first = userBlockService.block(userId, request);

            // when
            final UserBlockResponse second = userBlockService.block(userId, request);

            // then
            assertThat(second.id()).isEqualTo(first.id());
            assertThat(userBlockRepository.findAll()).hasSize(1);
        }
    }

    @Nested
    @DisplayName("unblock: 사용자 차단 해제")
    class Unblock {

        @Test
        @DisplayName("차단된 대상이면 row가 삭제된다")
        void unblock_whenBlocked_removesRow() {
            // given
            final Long userId = 1L;
            final UserBlock saved = userBlockRepository.save(UserBlock.create(userId, 2L));

            // when
            userBlockService.unblock(userId, 2L);

            // then
            assertThat(userBlockRepository.findById(saved.getId())).isEmpty();
        }

        @Test
        @DisplayName("차단되지 않은 대상이어도 예외 없이 완료된다 (멱등)")
        void unblock_whenNotBlocked_doesNotThrow() {
            // when & then
            userBlockService.unblock(1L, 9_999_999L);
        }
    }

    @Nested
    @DisplayName("getAllByUserId: 차단 목록 조회")
    class GetAllByUserId {

        @Test
        @DisplayName("응답 item에 차단된 사용자의 nickname/profileImage가 채워진다")
        void getAllByUserId_includesBlockedUserProfile() {
            // given
            final Long me = 1L;
            final User blocked = saveUser("타깃", "https://cdn.example.com/p.png");
            userBlockRepository.save(UserBlock.create(me, blocked.getId()));

            // when
            final UserBlocksResponse response = userBlockService.getAllByUserId(me, 0, 20);

            // then
            assertThat(response.items()).hasSize(1);
            final UserBlockItemResponse item = response.items().get(0);
            assertThat(item.blockedUserId()).isEqualTo(blocked.getId());
            assertThat(item.nickname()).isEqualTo("타깃");
            assertThat(item.profileImage()).isEqualTo("https://cdn.example.com/p.png");
            assertThat(response.hasNext()).isFalse();
        }

        @Test
        @DisplayName("차단된 사용자가 탈퇴(삭제)되었어도 차단 항목은 반환되며 nickname/profileImage는 null")
        void getAllByUserId_returnsItemWithNullProfileWhenBlockedUserMissing() {
            // given
            final Long me = 1L;
            final User withdrawn = saveUser("탈퇴자", "https://cdn.example.com/gone.png");
            final Long blockedUserId = withdrawn.getId();
            userBlockRepository.save(UserBlock.create(me, blockedUserId));
            userRepository.deleteById(blockedUserId);

            // when
            final UserBlocksResponse response = userBlockService.getAllByUserId(me, 0, 20);

            // then
            assertThat(response.items()).hasSize(1);
            final UserBlockItemResponse item = response.items().get(0);
            assertThat(item.blockedUserId()).isEqualTo(blockedUserId);
            assertThat(item.nickname()).isNull();
            assertThat(item.profileImage()).isNull();
        }

        @Test
        @DisplayName("page=0, size=2로 3건 중 2건을 반환하고 hasNext=true")
        void getAllByUserId_returnsFirstPageWithHasNextTrue() {
            // given
            final Long userId = 1L;
            userBlockRepository.save(UserBlock.create(userId, 10L));
            userBlockRepository.save(UserBlock.create(userId, 11L));
            userBlockRepository.save(UserBlock.create(userId, 12L));

            // when
            final UserBlocksResponse response =
                    userBlockService.getAllByUserId(userId, 0, 2);

            // then
            assertThat(response.items()).hasSize(2);
            assertThat(response.items()).allMatch(item -> item.blockedUserId() != null);
            assertThat(response.hasNext()).isTrue();
        }

        @Test
        @DisplayName("해당 userId의 항목이 없으면 빈 items + hasNext=false")
        void getAllByUserId_whenNone_returnsEmptyWithHasNextFalse() {
            // when
            final UserBlocksResponse response =
                    userBlockService.getAllByUserId(9_999_999L, 0, 20);

            // then
            assertThat(response.items()).isEmpty();
            assertThat(response.hasNext()).isFalse();
        }

        @Test
        @DisplayName("size가 MAX_SIZE(50)를 초과하면 내부적으로 50으로 clamp된다")
        void getAllByUserId_clampsOversizedRequestToMax() {
            // given
            final Long userId = 1L;
            for (long i = 0; i < 51; i++) {
                userBlockRepository.save(UserBlock.create(userId, 1000L + i));
            }

            // when
            final UserBlocksResponse response =
                    userBlockService.getAllByUserId(userId, 0, 999);

            // then
            assertThat(response.items()).hasSize(50);
            assertThat(response.hasNext()).isTrue();
        }

        @Test
        @DisplayName("size가 0이면 내부적으로 MIN_SIZE(1)로 clamp된다")
        void getAllByUserId_clampsZeroSizeToMin() {
            // given
            final Long userId = 1L;
            userBlockRepository.save(UserBlock.create(userId, 10L));
            userBlockRepository.save(UserBlock.create(userId, 11L));

            // when
            final UserBlocksResponse response =
                    userBlockService.getAllByUserId(userId, 0, 0);

            // then
            assertThat(response.items()).hasSize(1);
            assertThat(response.hasNext()).isTrue();
        }
    }

    private User saveUser(final String name, final String profileImageUrl) {
        return userRepository.save(
                User.createFromOAuth(
                        Provider.KAKAO,
                        "sub-" + name + "-" + UUID.randomUUID(),
                        new Name(name),
                        profileImageUrl
                )
        );
    }
}
