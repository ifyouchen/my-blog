package com.myblog.infrastructure.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;

/**
 * MyBatis 配置。
 *
 * @author Codex
 * @since 1.0.0
 */
@Configuration
@MapperScan({
    "com.myblog.infrastructure.repository.persistence.mapper",
    "com.myblog.growth.infrastructure.repository.persistence.mapper"
})
public class MyBatisConfig {
}
