package com.lingring.global.config;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.data.redis.connection.RedisConnectionFactory;

@SpringBootTest(webEnvironment = WebEnvironment.NONE)
@TestContainersTest
public abstract class ServiceIntegrationHelper {

    @Autowired
    private DataInitializer dataInitializer;

    @Autowired
    private RedisConnectionFactory redisConnectionFactory;

    @BeforeEach
    void setUp() {
        dataInitializer.deleteAll();
        flushRedis();
    }

    private void flushRedis() {
        redisConnectionFactory.getConnection().serverCommands().flushDb();
    }
}
