package com.myblog.infrastructure.storage.local;

import com.myblog.application.port.FileStorage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

/**
 * 基于本地文件系统的文件存储实现。
 * <p>当未配置 COS 时作为默认实现。</p>
 *
 * @author Codex
 * @since 1.0.0
 */
@Component
@ConditionalOnMissingBean(FileStorage.class)
public class LocalFileStorage implements FileStorage {

    private final Path uploadRoot;

    public LocalFileStorage(@Value("${my-blog.upload-dir:uploads}") String uploadDir) {
        this.uploadRoot = Paths.get(uploadDir).toAbsolutePath().normalize();
    }

    @Override
    public String upload(String key, InputStream inputStream, String contentType) {
        Path targetFile = uploadRoot.resolve(key).normalize();
        try {
            Files.createDirectories(targetFile.getParent());
            Files.copy(inputStream, targetFile, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException exception) {
            throw new RuntimeException("文件上传失败: " + key, exception);
        }
        return getUrl(key);
    }

    @Override
    public String upload(String key, InputStream inputStream, String contentType, long contentLength) {
        return upload(key, inputStream, contentType);
    }

    @Override
    public void delete(String key) {
        try {
            Path targetFile = uploadRoot.resolve(key).normalize();
            Files.deleteIfExists(targetFile);
        } catch (IOException exception) {
            throw new RuntimeException("文件删除失败: " + key, exception);
        }
    }

    @Override
    public String getUrl(String key) {
        return "/api/uploads/files/" + key;
    }
}
