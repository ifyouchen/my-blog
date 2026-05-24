package com.myblog.interfaces.rest.controller;

import com.myblog.application.port.FileStorage;
import com.myblog.application.port.PresignedUrlInfo;
import com.myblog.application.service.UploadRateLimiter;
import com.myblog.infrastructure.security.AuthContext;
import com.myblog.interfaces.rest.dto.request.PresignedUrlRequest;
import com.myblog.shared.exception.ApplicationException;
import com.myblog.shared.exception.ErrorCode;
import com.myblog.shared.result.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import javax.validation.Valid;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * 文件上传控制器。
 * <p>
 * 提供图片和附件的上传接口，支持头像、封面、正文图片、私信图片及通用附件上传。
 * 上传文件存储于腾讯云对象存储（COS），文件名使用 UUID 随机生成以防止命名冲突。
 * </p>
 *
 * @author Codex
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/uploads")
public class UploadController {

    private static final Logger LOGGER = LoggerFactory.getLogger(UploadController.class);

    /** 头像图片大小上限（2 MB）。 */
    private static final long AVATAR_MAX_BYTES = 2L * 1024L * 1024L;
    /** 封面图片大小上限（5 MB）。 */
    private static final long COVER_MAX_BYTES = 5L * 1024L * 1024L;
    /** 正文图片大小上限（5 MB）。 */
    private static final long CONTENT_IMAGE_MAX_BYTES = 5L * 1024L * 1024L;
    /** 私信图片大小上限（10 MB）。 */
    private static final long MESSAGE_IMAGE_MAX_BYTES = 10L * 1024L * 1024L;
    /** 通用附件大小上限（20 MB）。 */
    private static final long FILE_MAX_BYTES = 20L * 1024L * 1024L;
    /** 按日期分目录存储的格式化器。 */
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    /** 头像最长边。 */
    private static final int AVATAR_MAX_SIDE = 320;
    /** 封面最长边。 */
    private static final int COVER_MAX_SIDE = 1280;
    /** 正文图片最长边。 */
    private static final int CONTENT_IMAGE_MAX_SIDE = 1600;
    /** 私信图片最长边。 */
    private static final int MESSAGE_IMAGE_MAX_SIDE = 1600;
    /** 缩略图最长边。 */
    private static final int THUMBNAIL_MAX_SIDE = 480;
    /** 中等尺寸图片最长边。 */
    private static final int MEDIUM_MAX_SIDE = 960;
    /** JPEG 输出质量。 */
    private static final float JPEG_QUALITY = 0.82F;

    /** 预签名 URL 有效期（秒）。 */
    private static final long PRESIGNED_URL_EXPIRATION_SECONDS = 300L;

    /** 允许上传的附件 MIME 类型集合。 */
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

    /** 允许上传的附件扩展名集合。 */
    private static final Set<String> ALLOWED_ATTACHMENT_EXTENSIONS = new HashSet<>(Arrays.asList(
        ".pdf", ".doc", ".docx", ".xls", ".xlsx", ".ppt", ".pptx", ".zip", ".txt", ".md"
    ));

    private final FileStorage fileStorage;
    private final UploadRateLimiter uploadRateLimiter;

    public UploadController(FileStorage fileStorage, UploadRateLimiter uploadRateLimiter) {
        this.fileStorage = fileStorage;
        this.uploadRateLimiter = uploadRateLimiter;
    }

    /**
     * 上传图片。
     *
     * @param file  上传的图片文件
     * @param scope 图片用途（avatar/cover/content/message），决定大小限制
     * @return 包含访问 URL 的结果
     */
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

        uploadRateLimiter.acquire(userId, "image");
        validateImageFile(file, scope);

        String extension = resolveImageExtension(file);
        validateFileMagicBytes(file, extension);
        String key = buildObjectKey(extension);
        String contentType = resolveImageContentType(extension);

        try {
            byte[] originalBytes = file.getBytes();
            String url;
            String thumbnailUrl = null;
            String mediumUrl = null;

            if (canOptimize(extension)) {
                BufferedImage source = ImageIO.read(new ByteArrayInputStream(originalBytes));
                if (source != null) {
                    BufferedImage resized = resizeIfNeeded(source, resolveMaxSide(scope));
                    byte[] processed = encodeImage(resized, extension);
                    url = fileStorage.upload(key, new ByteArrayInputStream(processed), contentType, processed.length);

                    String thumbKey = buildVariantKey(key, "-thumb");
                    BufferedImage thumb = resizeIfNeeded(source, THUMBNAIL_MAX_SIDE);
                    byte[] thumbBytes = encodeImage(thumb, extension);
                    fileStorage.upload(thumbKey, new ByteArrayInputStream(thumbBytes), contentType, thumbBytes.length);
                    thumbnailUrl = fileStorage.getUrl(thumbKey);

                    String mediumKey = buildVariantKey(key, "-medium");
                    BufferedImage medium = resizeIfNeeded(source, MEDIUM_MAX_SIDE);
                    byte[] mediumBytes = encodeImage(medium, extension);
                    fileStorage.upload(mediumKey, new ByteArrayInputStream(mediumBytes), contentType, mediumBytes.length);
                    mediumUrl = fileStorage.getUrl(mediumKey);
                } else {
                    url = fileStorage.upload(key, new ByteArrayInputStream(originalBytes), contentType, originalBytes.length);
                }
            } else {
                url = fileStorage.upload(key, new ByteArrayInputStream(originalBytes), contentType, originalBytes.length);
            }

            Map<String, String> data = new HashMap<>();
            data.put("url", url);
            data.put("originalUrl", url);
            if (thumbnailUrl != null) {
                data.put("thumbnailUrl", thumbnailUrl);
                data.put("mediumUrl", mediumUrl);
            }

            LOGGER.info("COS 图片上传成功, userId={}, username={}, key={}",
                userId, AuthContext.getUsername(), key);
            return Result.success(data);
        } catch (IOException exception) {
            throw new ApplicationException(ErrorCode.SYSTEM_ERROR, "图片上传失败");
        }
    }

    /**
     * 上传附件。
     *
     * @param file 上传的附件文件
     * @return 包含访问 URL 和原始文件名的结果
     */
    @PostMapping("/files")
    public Result<Map<String, String>> uploadFile(@RequestParam("file") MultipartFile file) {
        Long userId = AuthContext.getRequiredUserId();
        if (userId == null) {
            throw new ApplicationException(ErrorCode.UNAUTHORIZED, "请先登录");
        }
        if (file == null || file.isEmpty()) {
            throw new ApplicationException(ErrorCode.PARAM_ERROR, "请选择要上传的文件");
        }

        uploadRateLimiter.acquire(userId, "file");
        validateAttachmentFile(file);

        String originalFilename = file.getOriginalFilename();
        String extension = "";
        if (StringUtils.hasText(originalFilename) && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf('.')).toLowerCase(Locale.ROOT);
        }

        String key = buildObjectKey(extension);
        String url;
        try (InputStream inputStream = file.getInputStream()) {
            url = fileStorage.upload(key, inputStream, file.getContentType(), file.getSize());
        } catch (IOException exception) {
            throw new ApplicationException(ErrorCode.SYSTEM_ERROR, "文件上传失败");
        }

        LOGGER.info("COS 文件上传成功, userId={}, username={}, key={}",
            userId, AuthContext.getUsername(), key);

        Map<String, String> data = new HashMap<>();
        data.put("url", url);
        data.put("filename", StringUtils.hasText(originalFilename) ? originalFilename
            : key.substring(key.lastIndexOf('/') + 1));
        return Result.success(data);
    }

    /**
     * 获取预签名 PUT URL，用于前端直传 COS。
     * <p>仅在 COS 模式下生效；本地存储模式返回 {@code uploadType: "proxy"}，前端降级为代理上传。</p>
     *
     * @param request 文件元信息
     * @return 预签名 URL 及图片访问地址
     */
    @PostMapping("/presigned-url")
    public Result<Map<String, Object>> getPresignedUrl(@Valid @RequestBody PresignedUrlRequest request) {
        Long userId = AuthContext.getRequiredUserId();
        if (userId == null) {
            throw new ApplicationException(ErrorCode.UNAUTHORIZED, "请先登录");
        }

        uploadRateLimiter.acquire(userId, "image");
        validateImageMetadata(request.getFileName(), request.getContentType(), request.getFileSize(), request.getScope());

        String extension = resolveExtensionFromName(request.getFileName());
        String key = buildObjectKey(extension);
        String contentType = resolveImageContentType(extension);

        PresignedUrlInfo presignedInfo = fileStorage.generatePresignedUrl(key, contentType, PRESIGNED_URL_EXPIRATION_SECONDS);

        Map<String, Object> data = new HashMap<>();

        if (presignedInfo == null) {
            data.put("uploadType", "proxy");
            return Result.success(data);
        }

        data.put("uploadType", "cos");
        data.put("presignedUrl", presignedInfo.getPresignedUrl());
        data.put("expiresAt", presignedInfo.getExpiresAt());

        String baseUrl = fileStorage.getUrl(key);
        data.put("originalUrl", baseUrl);

        String scope = request.getScope();
        if (canOptimize(extension)) {
            int maxSide = resolveMaxSide(scope);
            data.put("url", buildProcessedUrl(baseUrl, maxSide));
            data.put("thumbnailUrl", buildProcessedUrl(baseUrl, THUMBNAIL_MAX_SIDE));
            data.put("mediumUrl", buildProcessedUrl(baseUrl, MEDIUM_MAX_SIDE));
        } else {
            data.put("url", baseUrl);
        }

        LOGGER.info("COS 预签名 URL 生成成功, userId={}, username={}, key={}",
            userId, AuthContext.getUsername(), key);
        return Result.success(data);
    }

    // ========== Validation ==========

    /**
     * 校验图片文件的合法性（类型和大小）。
     *
     * @param file  上传的文件
     * @param scope 图片用途，决定大小限制
     */
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

    /**
     * 校验附件文件的合法性（大小和文件类型）。
     *
     * @param file 上传的文件
     */
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
            && ALLOWED_ATTACHMENT_EXTENSIONS.contains(extension.toLowerCase(Locale.ROOT));

        if (!validType) {
            throw new ApplicationException(ErrorCode.PARAM_ERROR,
                "不支持的文件类型，仅支持：pdf、doc、docx、xls、xlsx、ppt、pptx、zip、txt、md");
        }

        validateFileMagicBytes(file, extension);
    }

    /**
     * 校验文件魔数，防止文件内容与扩展名不匹配。
     *
     * @param file      上传的文件
     * @param extension 文件扩展名（含点号）
     */
    private void validateFileMagicBytes(MultipartFile file, String extension) {
        try (InputStream is = file.getInputStream()) {
            byte[] header = new byte[8];
            int read = is.read(header);
            if (read < 4) {
                throw new ApplicationException(ErrorCode.PARAM_ERROR, "文件内容无效");
            }
            boolean valid = false;
            switch (extension) {
                case ".jpg":
                case ".jpeg":
                    valid = header[0] == (byte) 0xFF && header[1] == (byte) 0xD8;
                    break;
                case ".png":
                    valid = header[0] == (byte) 0x89 && header[1] == (byte) 0x50
                        && header[2] == (byte) 0x4E && header[3] == (byte) 0x47;
                    break;
                case ".gif":
                    valid = header[0] == (byte) 0x47 && header[1] == (byte) 0x49
                        && header[2] == (byte) 0x46;
                    break;
                case ".webp":
                    valid = header[0] == (byte) 0x52 && header[1] == (byte) 0x49
                        && header[2] == (byte) 0x46 && header[3] == (byte) 0x46;
                    break;
                case ".pdf":
                    valid = header[0] == (byte) 0x25 && header[1] == (byte) 0x50
                        && header[2] == (byte) 0x44 && header[3] == (byte) 0x46;
                    break;
                case ".zip":
                    valid = (header[0] == (byte) 0x50 && header[1] == (byte) 0x4B
                        && header[2] == (byte) 0x03 && header[3] == (byte) 0x04);
                    break;
                default:
                    valid = true;
            }
            if (!valid) {
                throw new ApplicationException(ErrorCode.PARAM_ERROR, "文件内容与扩展名不匹配");
            }
        } catch (IOException e) {
            throw new ApplicationException(ErrorCode.SYSTEM_ERROR, "文件验证失败");
        }
    }

    // ========== Image Processing ==========

    /**
     * 将 BufferedImage 编码为字节数组，JPEG 使用有损压缩，PNG 使用无损编码。
     */
    private byte[] encodeImage(BufferedImage source, String extension) throws IOException {
        if (isJpeg(extension)) {
            return encodeJpeg(source);
        }
        return encodePng(source);
    }

    private byte[] encodeJpeg(BufferedImage source) throws IOException {
        BufferedImage rgb = toRgb(source);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageWriter writer = ImageIO.getImageWritersByFormatName("jpg").next();
        ImageWriteParam param = writer.getDefaultWriteParam();
        param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
        param.setCompressionQuality(JPEG_QUALITY);
        try (ImageOutputStream output = ImageIO.createImageOutputStream(baos)) {
            writer.setOutput(output);
            writer.write(null, new IIOImage(rgb, null, null), param);
        } finally {
            writer.dispose();
        }
        return baos.toByteArray();
    }

    private byte[] encodePng(BufferedImage source) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(source, "png", baos);
        return baos.toByteArray();
    }

    private BufferedImage resizeIfNeeded(BufferedImage source, int maxSide) {
        int width = source.getWidth();
        int height = source.getHeight();
        int longestSide = Math.max(width, height);
        if (longestSide <= maxSide) {
            return source;
        }
        double scale = maxSide / (double) longestSide;
        int targetWidth = Math.max(1, (int) Math.round(width * scale));
        int targetHeight = Math.max(1, (int) Math.round(height * scale));
        BufferedImage resized = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = resized.createGraphics();
        graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        graphics.drawImage(source, 0, 0, targetWidth, targetHeight, null);
        graphics.dispose();
        return resized;
    }

    private BufferedImage toRgb(BufferedImage source) {
        if (source.getType() == BufferedImage.TYPE_INT_RGB) {
            return source;
        }
        BufferedImage rgb = new BufferedImage(source.getWidth(), source.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = rgb.createGraphics();
        graphics.drawImage(source, 0, 0, java.awt.Color.WHITE, null);
        graphics.dispose();
        return rgb;
    }

    private int resolveMaxSide(String scope) {
        if ("avatar".equalsIgnoreCase(scope)) {
            return AVATAR_MAX_SIDE;
        }
        if ("content".equalsIgnoreCase(scope)) {
            return CONTENT_IMAGE_MAX_SIDE;
        }
        if ("message".equalsIgnoreCase(scope)) {
            return MESSAGE_IMAGE_MAX_SIDE;
        }
        return COVER_MAX_SIDE;
    }

    private boolean canOptimize(String extension) {
        return isJpeg(extension) || ".png".equalsIgnoreCase(extension);
    }

    private boolean isJpeg(String extension) {
        return ".jpg".equalsIgnoreCase(extension) || ".jpeg".equalsIgnoreCase(extension);
    }

    // ========== Presigned URL Metadata Validation ==========

    /**
     * 校验图片文件的元信息（不读取文件内容）。
     *
     * @param fileName    原始文件名
     * @param contentType MIME 类型
     * @param fileSize    文件大小（字节）
     * @param scope       图片用途
     */
    private void validateImageMetadata(String fileName, String contentType, Long fileSize, String scope) {
        if (contentType == null || !contentType.toLowerCase(Locale.ROOT).startsWith("image/")) {
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

        if (fileSize != null && fileSize > maxBytes) {
            throw new ApplicationException(ErrorCode.PARAM_ERROR, message);
        }
    }

    /**
     * 从文件名中解析扩展名。
     */
    private String resolveExtensionFromName(String fileName) {
        if (StringUtils.hasText(fileName) && fileName.contains(".")) {
            return fileName.substring(fileName.lastIndexOf('.')).toLowerCase(Locale.ROOT);
        }
        return ".jpg";
    }

    /**
     * 构建带 COS imageMogr2 图片处理参数的 URL。
     *
     * @param baseUrl 原始对象 URL
     * @param maxSide 最长边限制（像素）
     * @return 处理后 URL
     */
    private String buildProcessedUrl(String baseUrl, int maxSide) {
        return baseUrl + "?imageMogr2/thumbnail/!" + maxSide + "x" + maxSide + "r/quality/82";
    }

    // ========== Key / URL Helpers ==========

    /**
     * 解析图片扩展名，优先从原始文件名提取，否则根据 Content-Type 推断。
     */
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

    private String buildObjectKey(String extension) {
        return "myblog/" + DATE_FORMATTER.format(LocalDate.now()) + "/" + UUID.randomUUID().toString().replace("-", "") + extension;
    }

    private String buildVariantKey(String key, String suffix) {
        int dotIndex = key.lastIndexOf('.');
        if (dotIndex < 0) {
            return key + suffix;
        }
        return key.substring(0, dotIndex) + suffix + key.substring(dotIndex);
    }

    private String resolveImageContentType(String extension) {
        if (".jpg".equalsIgnoreCase(extension) || ".jpeg".equalsIgnoreCase(extension)) {
            return "image/jpeg";
        }
        if (".png".equalsIgnoreCase(extension)) {
            return "image/png";
        }
        if (".gif".equalsIgnoreCase(extension)) {
            return "image/gif";
        }
        if (".webp".equalsIgnoreCase(extension)) {
            return "image/webp";
        }
        return "application/octet-stream";
    }
}
