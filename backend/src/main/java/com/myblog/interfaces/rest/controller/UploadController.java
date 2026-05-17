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

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
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

/**
 * 文件上传控制器。
 * <p>
 * 提供图片和附件的上传接口，支持头像、封面、正文图片、私信图片及通用附件上传。
 * 上传文件按年月目录归档存储，文件名使用 UUID 随机生成以防止命名冲突。
 * </p>
 *
 * @author Codex
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/uploads")
public class UploadController {

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
    /** 按年月分目录存储的格式化器。 */
    private static final DateTimeFormatter MONTH_FORMATTER = DateTimeFormatter.ofPattern("yyyy/MM");
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

    /** 文件上传根目录（绝对路径）。 */
    private final Path uploadRoot;

    public UploadController(@Value("${my-blog.upload-dir:uploads}") String uploadDir) {
        this.uploadRoot = Paths.get(uploadDir).toAbsolutePath().normalize();
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

        validateImageFile(file, scope);

        String extension = resolveImageExtension(file);
        LocalDate today = LocalDate.now();
        String relativeFolder = MONTH_FORMATTER.format(today);
        String fileName = UUID.randomUUID().toString().replace("-", "") + extension;
        Path targetDir = uploadRoot.resolve(relativeFolder).normalize();
        Path targetFile = targetDir.resolve(fileName);

        try {
            Files.createDirectories(targetDir);
            saveOptimizedImage(file, scope, extension, targetFile);
        } catch (IOException exception) {
            throw new ApplicationException(ErrorCode.SYSTEM_ERROR, "图片上传失败");
        }

        Map<String, String> data = new HashMap<>();
        String fileUrl = "/api/uploads/files/" + relativeFolder + "/" + fileName;
        data.put("url", fileUrl);
        data.put("originalUrl", fileUrl);
        if (canOptimize(extension)) {
            putVariantUrls(data, relativeFolder, fileName);
        }
        return Result.success(data);
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
            || ALLOWED_ATTACHMENT_EXTENSIONS.contains(extension.toLowerCase(Locale.ROOT));

        if (!validType) {
            throw new ApplicationException(ErrorCode.PARAM_ERROR,
                "不支持的文件类型，仅支持：pdf、doc、docx、xls、xlsx、ppt、pptx、zip、txt、md");
        }
    }

    /**
     * 解析图片扩展名，优先从原始文件名提取，否则根据 Content-Type 推断。
     *
     * @param file 上传的文件
     * @return 扩展名（含点号，如 {@code .jpg}）
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

    /**
     * 保存优化后的图片。
     *
     * @param file 上传文件
     * @param scope 图片用途
     * @param extension 文件扩展名
     * @param targetFile 目标文件
     * @throws IOException 保存失败时抛出
     */
    private void saveOptimizedImage(MultipartFile file, String scope, String extension, Path targetFile)
        throws IOException {
        if (!canOptimize(extension)) {
            copyOriginal(file, targetFile);
            return;
        }
        BufferedImage source;
        try (InputStream inputStream = file.getInputStream()) {
            source = ImageIO.read(inputStream);
        }
        if (source == null) {
            copyOriginal(file, targetFile);
            return;
        }
        BufferedImage output = resizeIfNeeded(source, resolveMaxSide(scope));
        if (isJpeg(extension)) {
            writeJpeg(output, targetFile);
            saveImageVariants(output, targetFile, extension);
            return;
        }
        ImageIO.write(output, "png", targetFile.toFile());
        saveImageVariants(output, targetFile, extension);
    }

    private void saveImageVariants(BufferedImage source, Path targetFile, String extension) throws IOException {
        writeVariant(source, targetFile, extension, "-thumb", THUMBNAIL_MAX_SIDE);
        writeVariant(source, targetFile, extension, "-medium", MEDIUM_MAX_SIDE);
    }

    private void writeVariant(BufferedImage source, Path targetFile, String extension, String suffix, int maxSide)
        throws IOException {
        Path variantFile = resolveVariantFile(targetFile, suffix);
        BufferedImage variant = resizeIfNeeded(source, maxSide);
        if (isJpeg(extension)) {
            writeJpeg(variant, variantFile);
            return;
        }
        ImageIO.write(variant, "png", variantFile.toFile());
    }

    private void putVariantUrls(Map<String, String> data, String relativeFolder, String fileName) {
        String baseUrl = "/api/uploads/files/" + relativeFolder + "/";
        data.put("thumbnailUrl", baseUrl + buildVariantFileName(fileName, "-thumb"));
        data.put("mediumUrl", baseUrl + buildVariantFileName(fileName, "-medium"));
    }

    private Path resolveVariantFile(Path targetFile, String suffix) {
        return targetFile.resolveSibling(buildVariantFileName(targetFile.getFileName().toString(), suffix));
    }

    private String buildVariantFileName(String fileName, String suffix) {
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex < 0) {
            return fileName + suffix;
        }
        return fileName.substring(0, dotIndex) + suffix + fileName.substring(dotIndex);
    }

    private boolean canOptimize(String extension) {
        return isJpeg(extension) || ".png".equalsIgnoreCase(extension);
    }

    private boolean isJpeg(String extension) {
        return ".jpg".equalsIgnoreCase(extension) || ".jpeg".equalsIgnoreCase(extension);
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

    private void writeJpeg(BufferedImage source, Path targetFile) throws IOException {
        BufferedImage rgb = toRgb(source);
        ImageWriter writer = ImageIO.getImageWritersByFormatName("jpg").next();
        ImageWriteParam param = writer.getDefaultWriteParam();
        param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
        param.setCompressionQuality(JPEG_QUALITY);
        try (ImageOutputStream output = ImageIO.createImageOutputStream(targetFile.toFile())) {
            writer.setOutput(output);
            writer.write(null, new IIOImage(rgb, null, null), param);
        } finally {
            writer.dispose();
        }
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

    private void copyOriginal(MultipartFile file, Path targetFile) throws IOException {
        try (InputStream inputStream = file.getInputStream()) {
            Files.copy(inputStream, targetFile, StandardCopyOption.REPLACE_EXISTING);
        }
    }
}
