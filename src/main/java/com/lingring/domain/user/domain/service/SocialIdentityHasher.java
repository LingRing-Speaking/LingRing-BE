package com.lingring.domain.user.domain.service;

import com.lingring.domain.user.domain.Provider;
import com.lingring.global.error.ErrorCode;
import com.lingring.global.error.exception.InternalServerException;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.HexFormat;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class SocialIdentityHasher {

    private static final String ALGORITHM = "HmacSHA256";

    private final SecretKeySpec key;

    public SocialIdentityHasher(@Value("${withdrawn-identity.hmac-key}") final String secret) {
        this.key = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), ALGORITHM);
    }

    public String hash(final Provider provider, final String providerUserId) {
        final String identity = provider.name() + ":" + providerUserId;
        try {
            final Mac mac = Mac.getInstance(ALGORITHM);
            mac.init(key);
            return HexFormat.of().formatHex(mac.doFinal(identity.getBytes(StandardCharsets.UTF_8)));
        } catch (final GeneralSecurityException e) {
            throw new InternalServerException(ErrorCode.SERVER_ERROR, "소셜 계정 식별자 해시 계산에 실패했습니다.", e);
        }
    }
}
