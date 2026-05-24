package com.myblog.application.port;

import java.io.InputStream;

/**
 * 文件存储端口。
 * <p>
 * 定义文件上传、删除和 URL 生成等抽象操作，由基础设施层提供实现。
 * </p>
 *
 * @author Codex
 * @since 1.0.0
 */
public interface FileStorage {

    /**
     * 上传文件。
     *
     * @param key         对象键（例如 {@code 2025/01/uuid.jpg}）
     * @param inputStream 文件内容流
     * @param contentType 文件 MIME 类型
     * @return 文件的可访问完整 URL
     */
    String upload(String key, InputStream inputStream, String contentType);

    /**
     * 上传文件（指定内容长度）。
     *
     * @param key          对象键
     * @param inputStream  文件内容流
     * @param contentType  文件 MIME 类型
     * @param contentLength 内容长度（字节）
     * @return 文件的可访问完整 URL
     */
    String upload(String key, InputStream inputStream, String contentType, long contentLength);

    /**
     * 删除文件。
     *
     * @param key 对象键
     */
    void delete(String key);

    /**
     * 获取文件的完整访问 URL。
     *
     * @param key 对象键
     * @return 完整 URL
     */
    String getUrl(String key);

    /**
     * 生成预签名 PUT URL，用于前端直传对象存储。
     * <p>默认返回 {@code null}，表示不支持预签名 URL（降级为服务端代理上传）。</p>
     *
     * @param key               对象键
     * @param contentType       文件 MIME 类型
     * @param expirationSeconds URL 有效时长（秒）
     * @return 预签名 URL 信息，不支持时返回 {@code null}
     */
    default PresignedUrlInfo generatePresignedUrl(String key, String contentType, long expirationSeconds) {
        return null;
    }
}
