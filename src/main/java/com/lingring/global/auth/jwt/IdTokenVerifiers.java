package com.lingring.global.auth.jwt;

import com.lingring.domain.user.domain.Provider;
import com.lingring.global.error.ErrorCode;
import com.lingring.global.error.exception.BadRequestException;
import java.util.Map;

public class IdTokenVerifiers {

    private final Map<Provider, IdTokenVerifier> verifiers;

    public IdTokenVerifiers(final Map<Provider, IdTokenVerifier> verifiers) {
        this.verifiers = Map.copyOf(verifiers);
    }

    public IdTokenVerifier resolve(final Provider provider) {
        final IdTokenVerifier verifier = verifiers.get(provider);
        if (verifier == null) {
            throw new BadRequestException(
                    ErrorCode.NOT_SUPPORTED,
                    "지원하지 않는 provider입니다: %s".formatted(provider.name())
            );
        }
        return verifier;
    }
}