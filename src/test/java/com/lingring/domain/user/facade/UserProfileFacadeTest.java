package com.lingring.domain.user.facade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.lingring.domain.friend.dao.FriendshipRepository;
import com.lingring.domain.friend.domain.FriendRelation;
import com.lingring.domain.friend.domain.Friendship;
import com.lingring.domain.user.dao.UserRepository;
import com.lingring.domain.user.dao.UserStatsRepository;
import com.lingring.domain.user.domain.Provider;
import com.lingring.domain.user.domain.User;
import com.lingring.domain.user.domain.UserStats;
import com.lingring.domain.user.domain.vo.Name;
import com.lingring.domain.user.dto.response.UserProfileResponse;
import com.lingring.global.config.ServiceIntegrationHelper;
import com.lingring.global.error.ErrorCode;
import com.lingring.global.error.exception.NotFoundException;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class UserProfileFacadeTest extends ServiceIntegrationHelper {

    @Autowired
    private UserProfileFacade userProfileFacade;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserStatsRepository userStatsRepository;

    @Autowired
    private FriendshipRepository friendshipRepository;

    @Nested
    @DisplayName("getUserProfile: 프로필 + 관계 조회")
    class GetUserProfile {

        @Test
        @DisplayName("관계가 없는 사용자를 조회하면 프로필과 relation=NONE을 반환한다")
        void getUserProfile_whenNoRelation_returnsProfileWithNone() {
            // given
            final User me = saveUserWithStats("미나", null);
            final String profileImageUrl = "https://cdn.example.com/j.png";
            final User target = saveUserWithStats("지우", profileImageUrl);

            // when
            final UserProfileResponse response = userProfileFacade.getUserProfile(me.getId(), target.getId());

            // then
            assertThat(response.id()).isEqualTo(target.getId());
            assertThat(response.nickname()).isEqualTo("지우");
            assertThat(response.profileImage()).isEqualTo(profileImageUrl);
            assertThat(response.level()).isNotNull();
            assertThat(response.mannerTemperature()).isNotNull();
            assertThat(response.relation()).isEqualTo(FriendRelation.NONE);
        }

        @Test
        @DisplayName("이미 친구인 사용자를 조회하면 relation=FRIEND를 반환한다")
        void getUserProfile_whenFriend_returnsFriendRelation() {
            // given
            final User me = saveUserWithStats("미나", null);
            final User target = saveUserWithStats("지우", null);
            final Friendship accepted = Friendship.request(me.getId(), target.getId());
            accepted.accept(target.getId());
            friendshipRepository.save(accepted);

            // when
            final UserProfileResponse response = userProfileFacade.getUserProfile(me.getId(), target.getId());

            // then
            assertThat(response.relation()).isEqualTo(FriendRelation.FRIEND);
        }

        @Test
        @DisplayName("자기 자신을 조회하면 relation=SELF를 반환한다")
        void getUserProfile_whenSelf_returnsSelfRelation() {
            // given
            final User me = saveUserWithStats("미나", null);

            // when
            final UserProfileResponse response = userProfileFacade.getUserProfile(me.getId(), me.getId());

            // then
            assertThat(response.relation()).isEqualTo(FriendRelation.SELF);
        }

        @Test
        @DisplayName("대상 사용자가 없으면 USER_NOT_FOUND 예외가 발생한다 (404)")
        void getUserProfile_whenTargetNotFound_throwsNotFound() {
            // given
            final User me = saveUserWithStats("미나", null);
            final Long missingId = 9_999_999L;

            // when & then
            assertThatThrownBy(() -> userProfileFacade.getUserProfile(me.getId(), missingId))
                    .isInstanceOf(NotFoundException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.USER_NOT_FOUND);
        }
    }

    private User saveUserWithStats(final String name, final String profileImageUrl) {
        final User saved = userRepository.save(
                User.createFromOAuth(
                        Provider.KAKAO,
                        "sub-" + name + "-" + UUID.randomUUID(),
                        new Name(name),
                        profileImageUrl
                )
        );
        userStatsRepository.save(UserStats.create(saved.getId()));
        return saved;
    }
}
