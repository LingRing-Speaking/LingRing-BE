package com.lingring.domain.auth.service;

import com.lingring.domain.user.domain.Provider;

public record VerifiedIdToken(
        Provider provider,
        String providerUserId
) {
}
