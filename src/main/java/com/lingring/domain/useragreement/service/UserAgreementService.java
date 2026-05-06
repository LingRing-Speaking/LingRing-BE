package com.lingring.domain.useragreement.service;

import com.lingring.domain.user.domain.User;
import com.lingring.domain.user.service.UserService;
import com.lingring.domain.useragreement.domain.AgreementItem;
import com.lingring.domain.useragreement.dto.request.AgreementCreateRequest;
import com.lingring.domain.useragreement.dto.response.AgreementResponse;
import com.lingring.global.error.ErrorCode;
import com.lingring.global.error.exception.BadRequestException;
import com.lingring.global.util.DateTimeProvider;
import java.time.LocalDateTime;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserAgreementService {

    private final UserService userService;
    private final DateTimeProvider timeProvider;

    @Transactional
    public AgreementResponse accept(final Long userId, final AgreementCreateRequest request) {
        requireAllRequiredItems(request.agreedItems());
        final User user = userService.getById(userId);
        user.markAgreed(request.termsVersion(), timeProvider.now());
        return AgreementResponse.from(user);
    }

    private void requireAllRequiredItems(final Set<AgreementItem> agreed) {
        if (!agreed.containsAll(AgreementItem.required())) {
            throw new BadRequestException(
                    ErrorCode.AGREEMENT_ITEMS_INCOMPLETE,
                    "필수 동의 항목 4개(OVER14, TERMS, PRIVACY, VOICE_AI)가 모두 포함되어야 합니다."
            );
        }
    }
}
