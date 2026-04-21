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

    private AdminLogId id;
    private UserId adminUserId;
    private String adminUsername;
    private String operation;
    private String targetType;
    private Long targetId;
    private String detail;
    private String requestMethod;
    private String requestUri;
    private String ipAddress;
    private String userAgent;
    private String resultStatus;
    private String beforeSnapshot;
    private String afterSnapshot;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime deletedAt;
    private Integer version;

    private AdminLog() {
    }

    /**
     * 创建管理员操作日志。
     *
     * @param id 日志 ID
     * @param adminUserId 管理员 ID
     * @param adminUsername 管理员用户名
     * @param operation 操作类型
     * @param targetType 目标类型
     * @param targetId 目标 ID
     * @param detail 操作详情
     * @param requestMethod 请求方法
     * @param requestUri 请求路径
     * @param ipAddress IP 地址
     * @param userAgent 用户代理
     * @param resultStatus 结果状态
     * @param beforeSnapshot 变更前快照
     * @param afterSnapshot 变更后快照
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
     * @param id 日志 ID
     * @param adminUserId 管理员 ID
     * @param adminUsername 管理员用户名
     * @param operation 操作类型
     * @param targetType 目标类型
     * @param targetId 目标 ID
     * @param detail 操作详情
     * @param requestMethod 请求方法
     * @param requestUri 请求路径
     * @param ipAddress IP 地址
     * @param userAgent 用户代理
     * @param resultStatus 结果状态
     * @param beforeSnapshot 变更前快照
     * @param afterSnapshot 变更后快照
     * @param createdAt 创建时间
     * @param updatedAt 更新时间
     * @param deletedAt 删除时间
     * @param version 版本号
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

    public AdminLogId getId() {
        return id;
    }

    public UserId getAdminUserId() {
        return adminUserId;
    }

    public String getAdminUsername() {
        return adminUsername;
    }

    public String getOperation() {
        return operation;
    }

    public String getTargetType() {
        return targetType;
    }

    public Long getTargetId() {
        return targetId;
    }

    public String getDetail() {
        return detail;
    }

    public String getRequestMethod() {
        return requestMethod;
    }

    public String getRequestUri() {
        return requestUri;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public String getResultStatus() {
        return resultStatus;
    }

    public String getBeforeSnapshot() {
        return beforeSnapshot;
    }

    public String getAfterSnapshot() {
        return afterSnapshot;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public LocalDateTime getDeletedAt() {
        return deletedAt;
    }

    public Integer getVersion() {
        return version;
    }
}
