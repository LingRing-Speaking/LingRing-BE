package com.lingring.domain.push.service;

import com.lingring.domain.push.dao.DeviceTokenRepository;
import com.lingring.domain.push.domain.DeviceToken;
import com.lingring.domain.push.domain.Platform;
import com.lingring.domain.push.dto.request.DeviceTokenRegisterRequest;
import com.lingring.domain.push.dto.request.DeviceTokenUnregisterRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DeviceTokenService {

    private final DeviceTokenRepository deviceTokenRepository;

    @Transactional
    public void register(final Long userId, final DeviceTokenRegisterRequest request) {
        final Platform platform = Platform.from(request.platform());
        deviceTokenRepository.findByToken(request.token())
                .ifPresentOrElse(
                        deviceToken -> deviceToken.reassign(userId, platform),
                        () -> deviceTokenRepository.save(DeviceToken.register(userId, request.token(), platform))
                );
    }

    @Transactional
    public void unregister(final Long userId, final DeviceTokenUnregisterRequest request) {
        deviceTokenRepository.deleteByUserIdAndToken(userId, request.token());
    }
}
