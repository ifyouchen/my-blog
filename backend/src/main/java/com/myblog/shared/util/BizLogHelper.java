package com.myblog.shared.util;

import org.slf4j.MDC;

/**
 * 业务日志辅助工具，提供统一的业务日志格式化模板。
 *
 * <p>配合 SLF4J 参数化日志使用，不封装 {@code log.info()} 调用：
 * <pre>{@code
 * long _start = System.currentTimeMillis();
 * // ... 业务逻辑 ...
 * log.info("{} | {} {} | 入参({}) | 结果({}) | {}",
 *     BizLogHelper.trace(),
 *     BizLogHelper.who(userId, nickname),
 *     "创建评论",
 *     BizLogHelper.params("articleId", articleId),
 *     BizLogHelper.created("commentId", commentId),
 *     BizLogHelper.elapsed(_start));
 * }</pre>
 *
 * <p>日志输出格式：traceId=xxxxx | 用户[{userId}][{nickname}] {action} | 入参({key=val, ...}) | 结果({result}) | 耗时({elapsed}ms)
 *
 * <p><b>脱敏规则：</b>
 * <ul>
 *   <li>密码、token、验证码、JWT → {@code ****}</li>
 *   <li>邮箱 → 保留前3字符 + *** + @域名</li>
 *   <li>IP → 保留前两段，后两段变 {@code *}</li>
 *   <li>大文本 → 只输出长度和摘要</li>
 * </ul>
 */
public final class BizLogHelper {

    private BizLogHelper() {
    }

    // ======================== traceId ========================

    /**
     * traceId 前缀，从 MDC 中获取。
     *
     * @return "traceId=a1b2c3d4e5f6"
     */
    public static String trace() {
        String traceId = MDC.get("traceId");
        return traceId != null ? "traceId=" + traceId : "traceId=-";
    }

    // ======================== 用户身份 ========================

    /**
     * 用户身份片段。
     *
     * @param userId   用户 ID
     * @param nickname 用户昵称
     * @return "用户[1001][张三]"
     */
    public static String who(Long userId, String nickname) {
        return String.format("用户[%s][%s]", userId, nickname);
    }

    /**
     * 用户身份片段（无昵称场景）。
     *
     * @param userId 用户 ID
     * @return "用户[1001]"
     */
    public static String who(Long userId) {
        return String.format("用户[%s]", userId);
    }

    // ======================== 参数（自动脱敏） ========================

    private static final java.util.Set<String> SENSITIVE_KEYS = new java.util.HashSet<>(
        java.util.Arrays.asList("password", "pwd", "token", "secret", "jwt", "code", "emailCode",
            "resetToken", "accessToken", "refreshToken", "currentPassword", "newPassword")
    );
    private static final java.util.Set<String> EMAIL_KEYS = new java.util.HashSet<>(
        java.util.Arrays.asList("email", "mail")
    );
    private static final java.util.Set<String> IP_KEYS = new java.util.HashSet<>(
        java.util.Arrays.asList("ip", "ipAddress", "clientIp")
    );

    /**
     * 生成关键参数字符串（自动脱敏敏感字段）。
     *
     * <p>参数按 key-value 对传入：{@code params("articleId", 42, "password", "123456")} 输出 "articleId=42, password=****"。
     *
     * @param keyValues key1, val1, key2, val2, ...
     * @return "articleId=42, password=****"
     */
    public static String params(Object... keyValues) {
        if (keyValues == null || keyValues.length == 0) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < keyValues.length; i += 2) {
            if (sb.length() > 0) {
                sb.append(", ");
            }
            String key = String.valueOf(keyValues[i]);
            sb.append(key).append("=");
            Object val = (i + 1 < keyValues.length) ? keyValues[i + 1] : null;
            sb.append(safeParamValue(key, val));
        }
        return sb.toString();
    }

    private static String safeParamValue(String key, Object raw) {
        if (raw == null) {
            return "null";
        }
        String keyLower = key.toLowerCase();
        String str = raw.toString();
        // 密码/token/验证码等 → 直接掩码
        if (SENSITIVE_KEYS.contains(keyLower)) {
            return "****";
        }
        // 邮箱脱敏（按 key 名或值含 @ 都匹配）
        if (EMAIL_KEYS.contains(keyLower) || str.contains("@")) {
            return safeEmail(str);
        }
        // IP 脱敏（按 key 名或值符合 IP 格式都匹配）
        if (IP_KEYS.contains(keyLower) || str.matches("\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}")) {
            return safeIp(str);
        }
        // 大文本 → 仅输出长度
        if (str.length() > 200) {
            return "length=" + str.length();
        }
        return str;
    }

    /**
     * 邮箱脱敏：test@example.com → "tes***@example.com"。
     */
    public static String safeEmail(String email) {
        if (email == null || !email.contains("@")) {
            return email;
        }
        int atIdx = email.indexOf('@');
        String prefix = email.substring(0, atIdx);
        String domain = email.substring(atIdx);
        if (prefix.length() <= 1) {
            return "*" + domain;
        }
        return prefix.substring(0, Math.min(prefix.length(), 3)) + "***" + domain;
    }

    /**
     * IP 脱敏：192.168.1.1 → "192.168.*.*"。
     */
    public static String safeIp(String ip) {
        if (ip == null) {
            return null;
        }
        int dotCount = 0;
        int lastDot = -1;
        for (int i = 0; i < ip.length(); i++) {
            if (ip.charAt(i) == '.') {
                dotCount++;
                if (dotCount == 2) {
                    lastDot = i;
                }
            }
        }
        if (dotCount >= 2 && lastDot > 0) {
            return ip.substring(0, lastDot) + ".*.*";
        }
        return ip;
    }

    /**
     * 敏感值掩码。
     *
     * @return "****"
     */
    public static String mask() {
        return "****";
    }

    /**
     * 大文本摘要（仅用于不宜直接打印正文的字段）。
     *
     * @param content 文本内容
     * @return "length=1024" 或 "length=50, preview=这是..."
     */
    public static String contentMeta(String content) {
        if (content == null) {
            return "null";
        }
        int len = content.length();
        if (len <= 50) {
            return "length=" + len + ", text=\"" + content + "\"";
        }
        return "length=" + len + ", preview=\"" + content.substring(0, 30) + "...\"";
    }

    // ======================== 结果摘要 ========================

    /**
     * 原始结果描述。
     *
     * @param message 结果描述
     * @return "articleId=42, status=PUBLISHED"
     */
    public static String result(String message) {
        return message;
    }

    /**
     * 创建类操作结果。
     *
     * @param key   ID 字段名，如 "articleId"
     * @param id    ID 值
     * @param extra 额外字段，如 "status=DRAFT"
     * @return "articleId=42, status=DRAFT"
     */
    public static String created(String key, Object id, String... extra) {
        StringBuilder sb = new StringBuilder(key).append("=").append(id);
        for (String e : extra) {
            sb.append(", ").append(e);
        }
        return sb.toString();
    }

    /**
     * 状态变更结果。
     *
     * @param beforeStatus 变更前状态
     * @param afterStatus  变更后状态
     * @return "DRAFT -> PUBLISHED"
     */
    public static String statusChanged(String beforeStatus, String afterStatus) {
        return beforeStatus + " -> " + afterStatus;
    }

    /**
     * 登录结果（不输出实际 token）。
     *
     * @param userId 用户 ID
     * @return "tokenIssued=true, userId=1001"
     */
    public static String loggedIn(Long userId) {
        return "tokenIssued=true, userId=" + userId;
    }

    /**
     * 分页查询结果。
     *
     * @param page     页码
     * @param pageSize 每页大小
     * @param total    总数
     * @return "page=1, pageSize=10, total=42"
     */
    public static String paged(int page, int pageSize, long total) {
        return "page=" + page + ", pageSize=" + pageSize + ", total=" + total;
    }

    // ======================== 耗时 ========================

    /**
     * 耗时片段。
     *
     * @param startMillis 方法开始时的 {@link System#currentTimeMillis()}
     * @return "耗时(2ms)"
     */
    public static String elapsed(long startMillis) {
        return String.format("耗时(%dms)", System.currentTimeMillis() - startMillis);
    }
}
