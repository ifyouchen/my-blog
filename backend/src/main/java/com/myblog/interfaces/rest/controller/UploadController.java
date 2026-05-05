package com.myblog.interfaces.rest.controller;

import com.myblog.infrastructure.security.AuthContext;
import com.myblog.shared.exception.ApplicationException;
import com.myblog.shared.exception.ErrorCode;
import com.myblog.shared.result.Result;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("/api/uploads")
public class UploadController {

    private static final long AVATAR_MAX_BYTES = 2L * 1024L * 1024L;
    private static final long COVER_MAX_BYTES = 5L * 1024L * 1024L;
    private static final long CONTENT_IMAGE_MAX_BYTES = 5L * 1024L * 1024L;
    private static final long MESSAGE_IMAGE_MAX_BYTES = 10L * 1024L * 1024L;
    private static final long FILE_MAX_BYTES = 20L * 1024L * 1024L;
    private static final DateTimeFormatter MONTH_FORMATTER = DateTimeFormatter.ofPattern("yyyy/MM");

    private static final Set<String> ALLOWED_ATTACHMENT_TYPES = new HashSet<>(Arrays.asList(
        "application/pdf",
        "application/msword",
        "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
        "application/vnd.ms-excel",
        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
        "application/vnd.ms-powerpoint",
        "application/vnd.openxmlformats-officedocument.presentationml.presentation",
        "application/zip",
        "text/plain",
        "text/markdown"
    ));

    private static final Set<String> ALLOWED_ATTACHMENT_EXTENSIONS = new HashSet<>(Arrays.asList(
        ".pdf", ".doc", ".docx", ".xls", ".xlsx", ".ppt", ".pptx", ".zip", ".txt", ".md"
    ));

    private final Path uploadRoot;

    public UploadController(@Value("${my-blog.upload-dir:uploads}") String uploadDir) {
        this.uploadRoot = Paths.get(uploadDir).toAbsolutePath().normalize();
    }

    @PostMapping("/images")
    public Result<Map<String, String>> uploadImage(@RequestParam("file") MultipartFile file,
                                                   @RequestParam(defaultValue = "cover") String scope) {
        Long userId = AuthContext.getRequiredUserId();
        if (userId == null) {
            throw new ApplicationException(ErrorCode.UNAUTHORIZED, "请先登录");
        }
        if (file == null || file.isEmpty()) {
            throw new ApplicationException(ErrorCode.PARAM_ERROR, "请选择要上传的图片");
        }

        validateImageFile(file, scope);

        String extension = resolveImageExtension(file);
        LocalDate today = LocalDate.now();
        String relativeFolder = MONTH_FORMATTER.format(today);
        String fileName = UUID.randomUUID().toString().replace("-", "") + extension;
        Path targetDir = uploadRoot.resolve(relativeFolder).normalize();
        Path targetFile = targetDir.resolve(fileName);

        try {
            Files.createDirectories(targetDir);
            try (InputStream inputStream = file.getInputStream()) {
                Files.copy(inputStream, targetFile, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException exception) {
            throw new ApplicationException(ErrorCode.SYSTEM_ERROR, "图片上传失败");
        }

        Map<String, String> data = new HashMap<>();
        data.put("url", "/api/uploads/files/" + relativeFolder + "/" + fileName);
        return Result.success(data);
    }

    @PostMapping("/files")
    public Result<Map<String, String>> uploadFile(@RequestParam("file") MultipartFile file) {
        Long userId = AuthContext.getRequiredUserId();
        if (userId == null) {
            throw new ApplicationException(ErrorCode.UNAUTHORIZED, "请先登录");
        }
        if (file == null || file.isEmpty()) {
            throw new ApplicationException(ErrorCode.PARAM_ERROR, "请选择要上传的文件");
        }

        validateAttachmentFile(file);

        String originalFilename = file.getOriginalFilename();
        String extension = "";
        if (StringUtils.hasText(originalFilename) && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf('.')).toLowerCase(Locale.ROOT);
        }

        LocalDate today = LocalDate.now();
        String relativeFolder = MONTH_FORMATTER.format(today);
        String fileName = UUID.randomUUID().toString().replace("-", "") + extension;
        Path targetDir = uploadRoot.resolve(relativeFolder).normalize();
        Path targetFile = targetDir.resolve(fileName);

        try {
            Files.createDirectories(targetDir);
            try (InputStream inputStream = file.getInputStream()) {
                Files.copy(inputStream, targetFile, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException exception) {
            throw new ApplicationException(ErrorCode.SYSTEM_ERROR, "文件上传失败");
        }

        Map<String, String> data = new HashMap<>();
        data.put("url", "/api/uploads/files/" + relativeFolder + "/" + fileName);
        data.put("filename", StringUtils.hasText(originalFilename) ? originalFilename : fileName);
        return Result.success(data);
    }

    private void validateImageFile(MultipartFile file, String scope) {
        String contentType = StringUtils.hasText(file.getContentType()) ? file.getContentType() : "";
        if (!contentType.toLowerCase(Locale.ROOT).startsWith("image/")) {
            throw new ApplicationException(ErrorCode.PARAM_ERROR, "仅支持上传图片文件");
        }

        long maxBytes;
        String message;
        if ("avatar".equalsIgnoreCase(scope)) {
            maxBytes = AVATAR_MAX_BYTES;
            message = "头像图片不能超过 2MB";
        } else if ("content".equalsIgnoreCase(scope)) {
            maxBytes = CONTENT_IMAGE_MAX_BYTES;
            message = "正文图片不能超过 5MB";
        } else if ("message".equalsIgnoreCase(scope)) {
            maxBytes = MESSAGE_IMAGE_MAX_BYTES;
            message = "私信图片不能超过 10MB";
        } else {
            maxBytes = COVER_MAX_BYTES;
            message = "封面图片不能超过 5MB";
        }

        if (file.getSize() > maxBytes) {
            throw new ApplicationException(ErrorCode.PARAM_ERROR, message);
        }
    }

    private void validateAttachmentFile(MultipartFile file) {
        if (file.getSize() > FILE_MAX_BYTES) {
            throw new ApplicationException(ErrorCode.PARAM_ERROR, "附件不能超过 20MB");
        }

        String contentType = StringUtils.hasText(file.getContentType()) ? file.getContentType() : "";
        String originalFilename = file.getOriginalFilename();
        String extension = "";
        if (StringUtils.hasText(originalFilename) && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf('.')).toLowerCase(Locale.ROOT);
        }

        boolean validType = ALLOWED_ATTACHMENT_TYPES.contains(contentType.toLowerCase(Locale.ROOT))
            || ALLOWED_ATTACHMENT_EXTENSIONS.contains(extension.toLowerCase(Locale.ROOT));

        if (!validType) {
            throw new ApplicationException(ErrorCode.PARAM_ERROR,
                "不支持的文件类型，仅支持：pdf、doc、docx、xls、xlsx、ppt、pptx、zip、txt、md");
        }
    }

    private String resolveImageExtension(MultipartFile file) {
        String originalFilename = file.getOriginalFilename();
        if (StringUtils.hasText(originalFilename) && originalFilename.contains(".")) {
            return originalFilename.substring(originalFilename.lastIndexOf('.')).toLowerCase(Locale.ROOT);
        }
        String contentType = file.getContentType();
        if ("image/png".equalsIgnoreCase(contentType)) {
            return ".png";
        }
        if ("image/gif".equalsIgnoreCase(contentType)) {
            return ".gif";
        }
        if ("image/webp".equalsIgnoreCase(contentType)) {
            return ".webp";
        }
        return ".jpg";
    }
}
