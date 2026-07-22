package com.lingring.global.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
@EnableAsync
@Slf4j
public class AsyncConfig {

    public static final String USER_EVENT_EXECUTOR = "userEventTaskExecutor";

    private static final int CORE_POOL_SIZE = 1;
    private static final int MAX_POOL_SIZE = 2;
    private static final int QUEUE_CAPACITY = 1000;

    @Bean(USER_EVENT_EXECUTOR)
    public ThreadPoolTaskExecutor userEventTaskExecutor() {
        final ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(CORE_POOL_SIZE);
        executor.setMaxPoolSize(MAX_POOL_SIZE);
        executor.setQueueCapacity(QUEUE_CAPACITY);
        executor.setThreadNamePrefix("user-event-");
        // 수집은 best-effort — 큐 포화 시 유저 경로를 막지 않고 이벤트를 버린다
        executor.setRejectedExecutionHandler((task, pool) -> log.warn("user_event 수집 큐 포화 — 이벤트 1건 폐기"));
        return executor;
    }
}
