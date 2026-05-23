package com.myblog.infrastructure.storage.cos;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.myblog.application.port.FileStorage;
import com.myblog.infrastructure.config.CosProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

import java.io.InputStream;

/**
 * 基于腾讯云对象存储（COS）的文件存储实现。
 * <p>通过 S3 兼容 API 接入 COS 服务。</p>
 *
 * @author Codex
 * @since 1.0.0
 */
public class CosFileStorage implements FileStorage {

    private static final Logger LOG = LoggerFactory.getLogger(CosFileStorage.class);

    private final AmazonS3 s3Client;
    private final CosProperties properties;

    public CosFileStorage(AmazonS3 s3Client, CosProperties properties) {
        this.s3Client = s3Client;
        this.properties = properties;
    }

    @Override
    public String upload(String key, InputStream inputStream, String contentType) {
        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentType(contentType);
        s3Client.putObject(properties.getBucket(), key, inputStream, metadata);
        LOG.debug("COS 上传完成: key={}", key);
        return getUrl(key);
    }

    @Override
    public String upload(String key, InputStream inputStream, String contentType, long contentLength) {
        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentType(contentType);
        metadata.setContentLength(contentLength);
        s3Client.putObject(properties.getBucket(), key, inputStream, metadata);
        LOG.debug("COS 上传完成: key={}, size={}", key, contentLength);
        return getUrl(key);
    }

    @Override
    public void delete(String key) {
        s3Client.deleteObject(properties.getBucket(), key);
    }

    @Override
    public String getUrl(String key) {
        if (StringUtils.hasText(properties.getDomain())) {
            String domain = properties.getDomain();
            if (domain.endsWith("/")) {
                return domain + key;
            }
            return domain + "/" + key;
        }
        return "https://" + properties.getBucket() + ".cos." + properties.getRegion() + ".myqcloud.com/" + key;
    }
}
