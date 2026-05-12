package com.myblog.infrastructure.repository.persistence.entity;

import java.time.LocalDateTime;

/**
 * 管理员操作日志数据对象。
 *
 * @author Codex
 * @since 1.0.0
 */
public class AdminLogDO {

    /**
     * 日志 ID
     */
    private Long id;

    /**
     * 操作管理员用户 ID
     */
    private Long adminUserId;

    /**
     * 操作管理员用户名
     */
    private String adminUsername;

    /**
     * 操作类型
     */
    private String operation;

    /**
     * 操作目标类型
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
     * HTTP 请求方法
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
     * 操作结果状态
     */
    private String resultStatus;

    /**
     * 数据变更前快照（JSON 字符串）
     */
    private String beforeSnapshot;

    /**
     * 数据变更后快照（JSON 字符串）
     */
    private String afterSnapshot;

    /**
     * 记录创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 记录最后更新时间
     */
    private LocalDateTime updatedAt;

    /**
     * 软删除时间，为 null 表示未删除
     */
    private LocalDateTime deletedAt;

    /**
     * 乐观锁版本号
     */
    private Integer version;

    /**
     * 获取日志 ID。
     *
     * @return 日志 ID
     */
    public Long getId() {
        return id;
    }

    /**
     * 设置日志 ID。
     *
     * @param id 日志 ID
     */
    public void setId(Long id) {
        this.id = id;
    }

    /**
     * 获取操作管理员用户 ID。
     *
     * @return 管理员用户 ID
     */
    public Long getAdminUserId() {
        return adminUserId;
    }

    /**
     * 设置操作管理员用户 ID。
     *
     * @param adminUserId 管理员用户 ID
     */
    public void setAdminUserId(Long adminUserId) {
        this.adminUserId = adminUserId;
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
     * 设置操作管理员用户名。
     *
     * @param adminUsername 管理员用户名
     */
    public void setAdminUsername(String adminUsername) {
        this.adminUsername = adminUsername;
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
     * 设置操作类型。
     *
     * @param operation 操作类型
     */
    public void setOperation(String operation) {
        this.operation = operation;
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
     * 设置操作目标类型。
     *
     * @param targetType 操作目标类型
     */
    public void setTargetType(String targetType) {
        this.targetType = targetType;
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
     * 设置操作目标 ID。
     *
     * @param targetId 操作目标 ID
     */
    public void setTargetId(Long targetId) {
        this.targetId = targetId;
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
     * 设置操作详情描述。
     *
     * @param detail 操作详情描述
     */
    public void setDetail(String detail) {
        this.detail = detail;
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
     * 设置 HTTP 请求方法。
     *
     * @param requestMethod HTTP 请求方法
     */
    public void setRequestMethod(String requestMethod) {
        this.requestMethod = requestMethod;
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
     * 设置 HTTP 请求路径。
     *
     * @param requestUri HTTP 请求路径
     */
    public void setRequestUri(String requestUri) {
        this.requestUri = requestUri;
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
     * 设置操作来源 IP 地址。
     *
     * @param ipAddress IP 地址
     */
    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
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
     * 设置浏览器 User-Agent。
     *
     * @param userAgent User-Agent
     */
    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
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
     * 设置操作结果状态。
     *
     * @param resultStatus 操作结果状态
     */
    public void setResultStatus(String resultStatus) {
        this.resultStatus = resultStatus;
    }

    /**
     * 获取数据变更前快照（JSON 字符串）。
     *
     * @return 变更前快照
     */
    public String getBeforeSnapshot() {
        return beforeSnapshot;
    }

    /**
     * 设置数据变更前快照（JSON 字符串）。
     *
     * @param beforeSnapshot 变更前快照
     */
    public void setBeforeSnapshot(String beforeSnapshot) {
        this.beforeSnapshot = beforeSnapshot;
    }

    /**
     * 获取数据变更后快照（JSON 字符串）。
     *
     * @return 变更后快照
     */
    public String getAfterSnapshot() {
        return afterSnapshot;
    }

    /**
     * 设置数据变更后快照（JSON 字符串）。
     *
     * @param afterSnapshot 变更后快照
     */
    public void setAfterSnapshot(String afterSnapshot) {
        this.afterSnapshot = afterSnapshot;
    }

    /**
     * 获取记录创建时间。
     *
     * @return 创建时间
     */
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    /**
     * 设置记录创建时间。
     *
     * @param createdAt 创建时间
     */
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    /**
     * 获取记录最后更新时间。
     *
     * @return 最后更新时间
     */
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    /**
     * 设置记录最后更新时间。
     *
     * @param updatedAt 最后更新时间
     */
    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    /**
     * 获取软删除时间。
     *
     * @return 删除时间，未删除则为 null
     */
    public LocalDateTime getDeletedAt() {
        return deletedAt;
    }

    /**
     * 设置软删除时间。
     *
     * @param deletedAt 删除时间
     */
    public void setDeletedAt(LocalDateTime deletedAt) {
        this.deletedAt = deletedAt;
    }

    /**
     * 获取乐观锁版本号。
     *
     * @return 版本号
     */
    public Integer getVersion() {
        return version;
    }

    /**
     * 设置乐观锁版本号。
     *
     * @param version 版本号
     */
    public void setVersion(Integer version) {
        this.version = version;
    }
}
