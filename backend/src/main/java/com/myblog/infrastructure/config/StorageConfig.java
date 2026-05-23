package com.myblog.infrastructure.config;

import com.amazonaws.services.s3.AmazonS3;
import com.myblog.application.port.FileStorage;
import com.myblog.infrastructure.storage.cos.CosFileStorage;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * 文件存储策略配置。
 * <p>当 S3 客户端（COS）可用时使用 COS 存储，否则降级为本地存储。</p>
 *
 * @author Codex
 * @since 1.0.0
 */
@Configuration
public class StorageConfig {

    /**
     * 创建 COS 文件存储实现。
     *
     * @param s3Client   S3 兼容客户端
     * @param properties COS 配置属性
     * @return COS 文件存储
     */
    @Bean
    @Primary
    @ConditionalOnBean(AmazonS3.class)
    public FileStorage cosFileStorage(AmazonS3 s3Client, CosProperties properties) {
        return new CosFileStorage(s3Client, properties);
    }
}
