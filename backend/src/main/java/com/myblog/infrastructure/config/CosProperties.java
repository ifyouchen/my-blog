package com.myblog.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 腾讯云对象存储（COS）配置属性。
 *
 * @author Codex
 * @since 1.0.0
 */
@ConfigurationProperties(prefix = "my-blog.cos")
public class CosProperties {

    /** 腾讯云 API 密钥 SecretId。 */
    private String secretId;

    /** 腾讯云 API 密钥 SecretKey。 */
    private String secretKey;

    /** COS 存储桶所在区域，例如 {@code ap-guangzhou}。 */
    private String region;

    /** COS 存储桶名称。 */
    private String bucket;

    /**
     * 自定义域名或 CDN 加速域名（可选）。
     * <p>配置后将使用此域名生成文件访问 URL，否则使用 COS 默认域名。</p>
     */
    private String domain;

    public String getSecretId() {
        return secretId;
    }

    public void setSecretId(String secretId) {
        this.secretId = secretId;
    }

    public String getSecretKey() {
        return secretKey;
    }

    public void setSecretKey(String secretKey) {
        this.secretKey = secretKey;
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public String getBucket() {
        return bucket;
    }

    public void setBucket(String bucket) {
        this.bucket = bucket;
    }

    public String getDomain() {
        return domain;
    }

    public void setDomain(String domain) {
        this.domain = domain;
    }
}
