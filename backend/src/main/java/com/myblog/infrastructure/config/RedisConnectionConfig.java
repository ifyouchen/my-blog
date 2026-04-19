package com.myblog.infrastructure.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;

/**
 * Redis 连接配置。
 *
 * @author Codex
 * @since 1.0.0
 */
@Configuration
public class RedisConnectionConfig {

    private static final Logger LOGGER = LoggerFactory.getLogger(RedisConnectionConfig.class);

    /**
     * 启动时检查 Redis 连接。
     *
     * @param redisConnectionFactory Redis 连接工厂
     * @return Redis 连接检查任务
     */
    @Bean
    public CommandLineRunner redisConnectionChecker(RedisConnectionFactory redisConnectionFactory) {
        return args -> {
            try (RedisConnection connection = redisConnectionFactory.getConnection()) {
                String pong = connection.ping();
                LOGGER.info("Redis连接检查完成，result={}", pong);
            } catch (Exception exception) {
                LOGGER.warn("Redis连接检查失败，请确认地址、端口和密码配置正确", exception);
            }
        };
    }
}
