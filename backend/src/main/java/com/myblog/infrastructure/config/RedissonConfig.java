package com.myblog.infrastructure.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.codec.JsonJacksonCodec;
import org.redisson.config.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Redisson 客户端配置。
 *
 * @author Codex
 * @since 1.0.0
 */
@Configuration
public class RedissonConfig {

    private static final Logger log = LoggerFactory.getLogger(RedissonConfig.class);

    @Bean(destroyMethod = "shutdown")
    public RedissonClient redissonClient(@Value("${spring.redis.host:127.0.0.1}") String host,
                                          @Value("${spring.redis.port:6379}") int port,
                                          @Value("${spring.redis.password:}") String password) {
        Config config = new Config();
        config.useSingleServer()
            .setAddress("redis://" + host + ":" + port);

        if (password != null && !password.isEmpty()) {
            config.useSingleServer().setPassword(password);
        }

        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        config.setCodec(new JsonJacksonCodec(mapper));
        log.info("Redisson client initialized: redis://{}:{}", host, port);
        return Redisson.create(config);
    }
}
