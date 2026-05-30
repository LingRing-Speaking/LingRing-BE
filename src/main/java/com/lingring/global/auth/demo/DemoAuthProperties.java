package com.lingring.global.auth.demo;

import java.util.Collections;
import java.util.Map;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Apple App Review 리뷰어용 demo 로그인 설정.
 * 토큰 → providerUserId 매핑으로 미리 시드된 demo user 의 JWT 를 발급한다.
 *
 * SIWA·Kakao 전용 앱이라 진짜 계정 발급이 2FA 때문에 비현실적이라 BE Test Account
 * Bypass 패턴을 사용한다. demo user 는 SQL 로 미리 INSERT 되어 있어야 하며,
 * provider 는 Provider.KAKAO 로 두되 providerUserId 를 영문 식별자(예: "REVIEWER_A")
 * 로 채워 일반 카카오 user(숫자 sub) 와 충돌하지 않도록 한다.
 */
@ConfigurationProperties(prefix = "auth.demo")
public record DemoAuthProperties(
        boolean enabled,
        Map<String, String> tokens
) {

    public DemoAuthProperties {
        tokens = tokens == null ? Map.of() : Collections.unmodifiableMap(tokens);
    }
}
