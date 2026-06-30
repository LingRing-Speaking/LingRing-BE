package com.lingring.domain.auth.facade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.lingring.domain.auth.dto.response.AuthTokenResponse;
import com.lingring.domain.auth.dto.response.AuthTokenResponse.UserSummary;
import com.lingring.domain.auth.service.AuthService;
import com.lingring.domain.user.domain.Provider;
import com.lingring.domain.user.domain.User;
import com.lingring.domain.user.domain.vo.Name;
import com.lingring.domain.user.service.UserService;
import com.lingring.global.auth.demo.DemoAuthProperties;
import com.lingring.global.error.ErrorCode;
import com.lingring.global.error.exception.ForbiddenException;
import com.lingring.global.error.exception.InternalServerException;
import com.lingring.global.error.exception.UnauthorizedException;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class DemoLoginFacadeTest {

    private final UserService userService = mock(UserService.class);
    private final AuthService authService = mock(AuthService.class);

    private DemoLoginFacade newFacade(final boolean enabled, final Map<String, String> tokens) {
        return new DemoLoginFacade(userService, authService, new DemoAuthProperties(enabled, tokens));
    }

    @Test
    @DisplayName("demo 로그인 비활성화 시 DEMO_LOGIN_DISABLED 예외")
    void demoLogin_whenDisabled_throwsForbidden() {
        final DemoLoginFacade facade = newFacade(false, Map.of("REVIEWER_A", "any-token"));

        assertThatThrownBy(() -> facade.demoLogin("any-token"))
                .isInstanceOf(ForbiddenException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.DEMO_LOGIN_DISABLED);
    }

    @Test
    @DisplayName("매핑되지 않은 토큰이면 INVALID_DEMO_TOKEN 예외")
    void demoLogin_whenTokenNotMapped_throwsUnauthorized() {
        final DemoLoginFacade facade = newFacade(true, Map.of("REVIEWER_A", "review-token-a"));

        assertThatThrownBy(() -> facade.demoLogin("wrong-token"))
                .isInstanceOf(UnauthorizedException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_DEMO_TOKEN);
    }

    @Test
    @DisplayName("매핑은 있는데 demo user 가 DB에 시드되지 않으면 DEMO_USER_NOT_SEEDED 예외")
    void demoLogin_whenUserNotSeeded_throwsInternalServerError() {
        final DemoLoginFacade facade = newFacade(true, Map.of("REVIEWER_A", "review-token-a"));
        given(userService.findByProvider(eq(Provider.KAKAO), eq("REVIEWER_A")))
                .willReturn(Optional.empty());

        assertThatThrownBy(() -> facade.demoLogin("review-token-a"))
                .isInstanceOf(InternalServerException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.DEMO_USER_NOT_SEEDED);
    }

    @Test
    @DisplayName("정상 토큰 + 시드된 user 면 AuthService 가 발급한 토큰을 반환한다")
    void demoLogin_whenValid_returnsAuthTokenResponse() {
        // given
        final DemoLoginFacade facade = newFacade(true, Map.of("REVIEWER_A", "review-token-a"));
        final User demoUser = User.createFromOAuth(Provider.KAKAO, "REVIEWER_A", new Name("Reviewer A"), null);
        given(userService.findByProvider(eq(Provider.KAKAO), eq("REVIEWER_A")))
                .willReturn(Optional.of(demoUser));
        final AuthTokenResponse expected = new AuthTokenResponse(
                "demo-access",
                "demo-refresh",
                new UserSummary(7L, "Reviewer A", null, false, "2026-06-30")
        );
        given(authService.issueTokensFor(demoUser)).willReturn(expected);

        // when
        final AuthTokenResponse response = facade.demoLogin("review-token-a");

        // then
        assertThat(response).isSameAs(expected);
        verify(authService).issueTokensFor(demoUser);
    }

    @Test
    @DisplayName("disabled 상태에선 user 조회조차 하지 않는다 (조기 차단)")
    void demoLogin_whenDisabled_doesNotQueryUserService() {
        final DemoLoginFacade facade = newFacade(false, Map.of("REVIEWER_A", "any-token"));

        assertThatThrownBy(() -> facade.demoLogin("any-token"))
                .isInstanceOf(ForbiddenException.class);

        verify(userService, never()).findByProvider(eq(Provider.KAKAO), eq("REVIEWER_A"));
    }

    @Test
    @DisplayName("매핑되지 않은 토큰일 때도 user 조회는 호출하지 않는다")
    void demoLogin_whenTokenInvalid_doesNotQueryUserService() {
        final DemoLoginFacade facade = newFacade(true, Map.of("REVIEWER_A", "review-token-a"));

        assertThatThrownBy(() -> facade.demoLogin("wrong-token"))
                .isInstanceOf(UnauthorizedException.class);

        verify(userService, never()).findByProvider(eq(Provider.KAKAO), eq("REVIEWER_A"));
    }
}
