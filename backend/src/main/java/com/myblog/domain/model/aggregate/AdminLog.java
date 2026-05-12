package com.myblog.domain.model.aggregate;

import com.myblog.domain.model.valueobject.AdminLogId;
import com.myblog.domain.model.valueobject.UserId;

import java.time.LocalDateTime;

/**
 * 管理员操作日志聚合根。
 *
 * @author Codex
 * @since 1.0.0
 */
public class AdminLog {

    /**
     * 日志 ID
     */
    private AdminLogId id;

    /**
     * 操作管理员用户 ID
     */
    private UserId adminUserId;

    /**
     * 操作管理员用户名
     */
    private String adminUsername;

    /**
     * 操作类型（如 CREATE_ARTICLE、DELETE_USER 等）
     */
    private String operation;

    /**
     * 操作目标类型（如 ARTICLE、USER、COMMENT 等）
     */
    private String targetType;

    /**
     * 操作目标 ID
     */
    private Long targetId;

    /**
     * 操作详情描述
     */
    private String detail;

    /**
     * HTTP 请求方法（GET / POST / PUT / DELETE 等）
     */
    private String requestMethod;

    /**
     * HTTP 请求路径
     */
    private String requestUri;

    /**
     * 操作来源 IP 地址
     */
    private String ipAddress;

    /**
     * 浏览器 User-Agent
     */
    private String userAgent;

    /**
     * 操作结果状态（SUCCESS / FAIL 等）
     */
    private String resultStatus;

    /**
     * 数据变更前的快照（JSON 字符串）
     */
    private String beforeSnapshot;

    /**
     * 数据变更后的快照（JSON 字符串）
     */
    private String afterSnapshot;

    /**
     * 日志创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 日志最后更新时间
     */
    private LocalDateTime updatedAt;

    /**
     * 日志软删除时间，为 null 表示未删除
     */
    private LocalDateTime deletedAt;

    /**
     * 乐观锁版本号
     */
    private Integer version;

    private AdminLog() {
    }

    /**
     * 创建管理员操作日志。
     *
     * @param id             日志 ID
     * @param adminUserId    管理员 ID
     * @param adminUsername  管理员用户名
     * @param operation      操作类型
     * @param targetType     目标类型
     * @param targetId       目标 ID
     * @param detail         操作详情
     * @param requestMethod  请求方法
     * @param requestUri     请求路径
     * @param ipAddress      IP 地址
     * @param userAgent      用户代理
     * @param resultStatus   结果状态
     * @param beforeSnapshot 变更前快照
     * @param afterSnapshot  变更后快照
     * @return 管理员操作日志
     */
    public static AdminLog create(Long id, UserId adminUserId, String adminUsername, String operation,
                                  String targetType, Long targetId, String detail, String requestMethod,
                                  String requestUri, String ipAddress, String userAgent, String resultStatus,
                                  String beforeSnapshot, String afterSnapshot) {
        AdminLog adminLog = new AdminLog();
        adminLog.id = new AdminLogId(id);
        adminLog.adminUserId = adminUserId;
        adminLog.adminUsername = adminUsername;
        adminLog.operation = operation;
        adminLog.targetType = targetType;
        adminLog.targetId = targetId;
        adminLog.detail = detail;
        adminLog.requestMethod = requestMethod;
        adminLog.requestUri = requestUri;
        adminLog.ipAddress = ipAddress;
        adminLog.userAgent = userAgent;
        adminLog.resultStatus = resultStatus;
        adminLog.beforeSnapshot = beforeSnapshot;
        adminLog.afterSnapshot = afterSnapshot;
        adminLog.createdAt = LocalDateTime.now();
        adminLog.updatedAt = adminLog.createdAt;
        adminLog.deletedAt = null;
        adminLog.version = 0;
        return adminLog;
    }

    /**
     * 从持久化数据还原管理员操作日志。
     *
     * @param id             日志 ID
     * @param adminUserId    管理员 ID
     * @param adminUsername  管理员用户名
     * @param operation      操作类型
     * @param targetType     目标类型
     * @param targetId       目标 ID
     * @param detail         操作详情
     * @param requestMethod  请求方法
     * @param requestUri     请求路径
     * @param ipAddress      IP 地址
     * @param userAgent      用户代理
     * @param resultStatus   结果状态
     * @param beforeSnapshot 变更前快照
     * @param afterSnapshot  变更后快照
     * @param createdAt      创建时间
     * @param updatedAt      更新时间
     * @param deletedAt      删除时间
     * @param version        版本号
     * @return 管理员操作日志
     */
    public static AdminLog restore(Long id, Long adminUserId, String adminUsername, String operation,
                                   String targetType, Long targetId, String detail, String requestMethod,
                                   String requestUri, String ipAddress, String userAgent, String resultStatus,
                                   String beforeSnapshot, String afterSnapshot, LocalDateTime createdAt,
                                   LocalDateTime updatedAt, LocalDateTime deletedAt, Integer version) {
        AdminLog adminLog = new AdminLog();
        adminLog.id = new AdminLogId(id);
        adminLog.adminUserId = new UserId(adminUserId);
        adminLog.adminUsername = adminUsername;
        adminLog.operation = operation;
        adminLog.targetType = targetType;
        adminLog.targetId = targetId;
        adminLog.detail = detail;
        adminLog.requestMethod = requestMethod;
        adminLog.requestUri = requestUri;
        adminLog.ipAddress = ipAddress;
        adminLog.userAgent = userAgent;
        adminLog.resultStatus = resultStatus;
        adminLog.beforeSnapshot = beforeSnapshot;
        adminLog.afterSnapshot = afterSnapshot;
        adminLog.createdAt = createdAt;
        adminLog.updatedAt = updatedAt;
        adminLog.deletedAt = deletedAt;
        adminLog.version = version;
        return adminLog;
    }

    /**
     * 获取日志 ID。
     *
     * @return 日志 ID
     */
    public AdminLogId getId() {
        return id;
    }

    /**
     * 获取操作管理员用户 ID。
     *
     * @return 管理员用户 ID
     */
    public UserId getAdminUserId() {
        return adminUserId;
    }

    /**
     * 获取操作管理员用户名。
     *
     * @return 管理员用户名
     */
    public String getAdminUsername() {
        return adminUsername;
    }

    /**
     * 获取操作类型。
     *
     * @return 操作类型
     */
    public String getOperation() {
        return operation;
    }

    /**
     * 获取操作目标类型。
     *
     * @return 操作目标类型
     */
    public String getTargetType() {
        return targetType;
    }

    /**
     * 获取操作目标 ID。
     *
     * @return 操作目标 ID
     */
    public Long getTargetId() {
        return targetId;
    }

    /**
     * 获取操作详情描述。
     *
     * @return 操作详情描述
     */
    public String getDetail() {
        return detail;
    }

    /**
     * 获取 HTTP 请求方法。
     *
     * @return HTTP 请求方法
     */
    public String getRequestMethod() {
        return requestMethod;
    }

    /**
     * 获取 HTTP 请求路径。
     *
     * @return HTTP 请求路径
     */
    public String getRequestUri() {
        return requestUri;
    }

    /**
     * 获取操作来源 IP 地址。
     *
     * @return IP 地址
     */
    public String getIpAddress() {
        return ipAddress;
    }

    /**
     * 获取浏览器 User-Agent。
     *
     * @return User-Agent
     */
    public String getUserAgent() {
        return userAgent;
    }

    /**
     * 获取操作结果状态。
     *
     * @return 操作结果状态
     */
    public String getResultStatus() {
        return resultStatus;
    }

    /**
     * 获取数据变更前的快照（JSON 字符串）。
     *
     * @return 变更前快照
     */
    public String getBeforeSnapshot() {
        return beforeSnapshot;
    }

    /**
     * 获取数据变更后的快照（JSON 字符串）。
     *
     * @return 变更后快照
     */
    public String getAfterSnapshot() {
        return afterSnapshot;
    }

    /**
     * 获取日志创建时间。
     *
     * @return 创建时间
     */
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    /**
     * 获取日志最后更新时间。
     *
     * @return 最后更新时间
     */
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    /**
     * 获取日志软删除时间。
     *
     * @return 删除时间，未删除则为 null
     */
    public LocalDateTime getDeletedAt() {
        return deletedAt;
    }

    /**
     * 获取乐观锁版本号。
     *
     * @return 版本号
     */
    public Integer getVersion() {
        return version;
    }
}
