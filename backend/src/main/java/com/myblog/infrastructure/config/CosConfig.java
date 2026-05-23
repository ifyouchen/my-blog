package com.myblog.infrastructure.config;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 腾讯云对象存储（COS）客户端配置。
 * <p>通过 S3 兼容协议接入 COS 服务。
 * 当 {@code my-blog.cos.secret-id} 配置不为空时初始化 S3 客户端，
 * 否则跳过，由本地文件存储作为降级方案。</p>
 *
 * @author Codex
 * @since 1.0.0
 */
@Configuration
@EnableConfigurationProperties(CosProperties.class)
@ConditionalOnExpression("'${my-blog.cos.secret-id:}' != ''")
public class CosConfig {

    private static final Logger LOG = LoggerFactory.getLogger(CosConfig.class);

    /**
     * 创建兼容 COS 的 S3 客户端。
     *
     * @param properties COS 配置属性
     * @return S3 兼容客户端
     */
    @Bean
    public AmazonS3 amazonS3(CosProperties properties) {
        BasicAWSCredentials credentials = new BasicAWSCredentials(properties.getSecretId(), properties.getSecretKey());
        String endpoint = "https://cos." + properties.getRegion() + ".myqcloud.com";
        AmazonS3 s3Client = AmazonS3ClientBuilder.standard()
            .withEndpointConfiguration(new AwsClientBuilder.EndpointConfiguration(endpoint, properties.getRegion()))
            .withCredentials(new AWSStaticCredentialsProvider(credentials))
            .withPathStyleAccessEnabled(true)
            .build();
        LOG.info("COS S3 客户端初始化完成，endpoint={}, bucket={}", endpoint, properties.getBucket());
        return s3Client;
    }
}
