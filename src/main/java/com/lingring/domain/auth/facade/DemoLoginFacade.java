package com.lingring.domain.auth.facade;

import com.lingring.domain.auth.dto.response.AuthTokenResponse;
import com.lingring.domain.auth.service.AuthService;
import com.lingring.domain.user.domain.Provider;
import com.lingring.domain.user.domain.User;
import com.lingring.domain.user.service.UserService;
import com.lingring.global.auth.demo.DemoAuthProperties;
import com.lingring.global.error.ErrorCode;
import com.lingring.global.error.exception.ForbiddenException;
import com.lingring.global.error.exception.InternalServerException;
import com.lingring.global.error.exception.UnauthorizedException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Apple App Review 리뷰어용 demo 로그인.
 *
 * <p>SIWA·Kakao 전용 앱이라 진짜 Apple ID/Kakao 계정 발급은 2FA 때문에 비현실적.
 * 표준 패턴인 BE Test Account Bypass — 토큰 → 미리 시드된 demo user 매핑으로
 * 자체 JWT 를 발급한다.
 *
 * <p>demo user 는 운영자가 SQL 로 미리 INSERT 해 두어야 하며, provider 는
 * {@link Provider#KAKAO} 를 재사용하되 providerUserId 를 영문 식별자(예:
 * "REVIEWER_A") 로 채워 일반 카카오 user(숫자 sub) 와 충돌하지 않는다.
 */
@Component
@RequiredArgsConstructor
public class DemoLoginFacade {

    private final UserService userService;
    private final AuthService authService;
    private final DemoAuthProperties demoProps;

    @Transactional(readOnly = true)
    public AuthTokenResponse demoLogin(final String token) {
        if (!demoProps.enabled()) {
            throw new ForbiddenException(
                    ErrorCode.DEMO_LOGIN_DISABLED,
                    "데모 로그인이 비활성화되어 있습니다."
            );
        }

        final String demoIdentifier = demoProps.tokens().get(token);
        if (demoIdentifier == null) {
            throw new UnauthorizedException(
                    ErrorCode.INVALID_DEMO_TOKEN,
                    "유효하지 않은 데모 토큰입니다."
            );
        }

        final User user = userService
                .findByProvider(Provider.KAKAO, demoIdentifier)
                .orElseThrow(() -> new InternalServerException(
                        ErrorCode.DEMO_USER_NOT_SEEDED,
                        "데모 사용자가 DB에 시드되지 않았습니다: " + demoIdentifier
                ));

        return authService.issueTokensFor(user);
    }
}
