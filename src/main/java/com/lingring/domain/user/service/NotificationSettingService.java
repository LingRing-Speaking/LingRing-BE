package com.lingring.domain.user.service;

import com.lingring.domain.user.dao.UserRepository;
import com.lingring.domain.user.domain.User;
import com.lingring.domain.user.dto.request.NotificationSettingUpdateRequest;
import com.lingring.domain.user.dto.response.NotificationSettingResponse;
import com.lingring.global.error.ErrorCode;
import com.lingring.global.error.exception.NotFoundException;
import com.lingring.global.util.DateTimeProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class NotificationSettingService {

    private final UserRepository userRepository;
    private final DateTimeProvider timeProvider;

    @Transactional
    public NotificationSettingResponse update(
            final Long userId,
            final NotificationSettingUpdateRequest request
    ) {
        final User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException(
                        ErrorCode.USER_NOT_FOUND,
                        "ID가 %d인 사용자를 찾을 수 없습니다.".formatted(userId)
                ));
        user.changeMarketingPushConsent(request.marketingPush(), timeProvider.now());
        return NotificationSettingResponse.from(user);
    }
}
