package com.lingring.global.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 통화 녹음 보유 기간 설정. 녹음 만료(EXPIRED) 상태 판정과 녹음 파기 배치가 공유하는 단일 기준값.
 */
@ConfigurationProperties(prefix = "call-recording")
public record CallRecordingRetentionProperties(
        int retentionDays
) {
}
