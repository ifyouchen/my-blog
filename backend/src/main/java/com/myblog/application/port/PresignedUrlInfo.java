package com.myblog.application.port;

/**
 * 预签名 URL 信息。
 * <p>封装由对象存储生成的预签名 PUT URL 及其过期时间。</p>
 *
 * @author Codex
 * @since 1.0.0
 */
public class PresignedUrlInfo {

    private final String presignedUrl;

    private final long expiresAt;

    /**
     * @param presignedUrl 预签名 PUT URL
     * @param expiresAt    过期时间戳（毫秒）
     */
    public PresignedUrlInfo(String presignedUrl, long expiresAt) {
        this.presignedUrl = presignedUrl;
        this.expiresAt = expiresAt;
    }

    public String getPresignedUrl() {
        return presignedUrl;
    }

    public long getExpiresAt() {
        return expiresAt;
    }
}
