package com.lingring.global.config;

import com.redis.testcontainers.RedisContainer;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.mysql.MySQLContainer;
import org.testcontainers.utility.DockerImageName;

@TestConfiguration(proxyBeanMethods = false)
@EnableConfigurationProperties({DatabaseProperties.class, RedisContainerProperties.class})
public class TestcontainersConfiguration {

    private static MySQLContainer mysqlInstance;
    private static RedisContainer redisInstance;

    @Bean(destroyMethod = "")
    @ServiceConnection
    static MySQLContainer mysqlContainer(final DatabaseProperties databaseProperties) {
        if (mysqlInstance == null) {
            mysqlInstance = new MySQLContainer(MySQLContainer.NAME + ":" + databaseProperties.version())
                    .withReuse(true)
                    .withDatabaseName(databaseProperties.databaseName())
                    .withUsername(databaseProperties.username())
                    .withPassword(databaseProperties.password());
            mysqlInstance.start();
        }
        return mysqlInstance;
    }

    @Bean(destroyMethod = "")
    @ServiceConnection
    static RedisContainer redisContainer(final RedisContainerProperties redisProperties) {
        if (redisInstance == null) {
            redisInstance = new RedisContainer(DockerImageName.parse(redisProperties.image()))
                    .withReuse(true);
            redisInstance.start();
        }
        return redisInstance;
    }
}
