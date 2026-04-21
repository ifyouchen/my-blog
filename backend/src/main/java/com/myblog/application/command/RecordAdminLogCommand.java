package com.myblog.application.command;

/**
 * 记录管理员操作日志命令。
 *
 * @author Codex
 * @since 1.0.0
 */
public class RecordAdminLogCommand {

    private Long adminUserId;
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
    private Object beforeSnapshot;
    private Object afterSnapshot;

    /**
     * 获取管理员用户 ID。
     *
     * @return 管理员用户 ID
     */
    public Long getAdminUserId() {
        return adminUserId;
    }

    /**
     * 设置管理员用户 ID。
     *
     * @param adminUserId 管理员用户 ID
     */
    public void setAdminUserId(Long adminUserId) {
        this.adminUserId = adminUserId;
    }

    /**
     * 获取管理员用户名。
     *
     * @return 管理员用户名
     */
    public String getAdminUsername() {
        return adminUsername;
    }

    /**
     * 设置管理员用户名。
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
     * 获取目标类型。
     *
     * @return 目标类型
     */
    public String getTargetType() {
        return targetType;
    }

    /**
     * 设置目标类型。
     *
     * @param targetType 目标类型
     */
    public void setTargetType(String targetType) {
        this.targetType = targetType;
    }

    /**
     * 获取目标 ID。
     *
     * @return 目标 ID
     */
    public Long getTargetId() {
        return targetId;
    }

    /**
     * 设置目标 ID。
     *
     * @param targetId 目标 ID
     */
    public void setTargetId(Long targetId) {
        this.targetId = targetId;
    }

    /**
     * 获取操作详情。
     *
     * @return 操作详情
     */
    public String getDetail() {
        return detail;
    }

    /**
     * 设置操作详情。
     *
     * @param detail 操作详情
     */
    public void setDetail(String detail) {
        this.detail = detail;
    }

    /**
     * 获取请求方法。
     *
     * @return 请求方法
     */
    public String getRequestMethod() {
        return requestMethod;
    }

    /**
     * 设置请求方法。
     *
     * @param requestMethod 请求方法
     */
    public void setRequestMethod(String requestMethod) {
        this.requestMethod = requestMethod;
    }

    /**
     * 获取请求路径。
     *
     * @return 请求路径
     */
    public String getRequestUri() {
        return requestUri;
    }

    /**
     * 设置请求路径。
     *
     * @param requestUri 请求路径
     */
    public void setRequestUri(String requestUri) {
        this.requestUri = requestUri;
    }

    /**
     * 获取 IP 地址。
     *
     * @return IP 地址
     */
    public String getIpAddress() {
        return ipAddress;
    }

    /**
     * 设置 IP 地址。
     *
     * @param ipAddress IP 地址
     */
    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    /**
     * 获取用户代理。
     *
     * @return 用户代理
     */
    public String getUserAgent() {
        return userAgent;
    }

    /**
     * 设置用户代理。
     *
     * @param userAgent 用户代理
     */
    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }

    /**
     * 获取结果状态。
     *
     * @return 结果状态
     */
    public String getResultStatus() {
        return resultStatus;
    }

    /**
     * 设置结果状态。
     *
     * @param resultStatus 结果状态
     */
    public void setResultStatus(String resultStatus) {
        this.resultStatus = resultStatus;
    }

    /**
     * 获取变更前快照。
     *
     * @return 变更前快照
     */
    public Object getBeforeSnapshot() {
        return beforeSnapshot;
    }

    /**
     * 设置变更前快照。
     *
     * @param beforeSnapshot 变更前快照
     */
    public void setBeforeSnapshot(Object beforeSnapshot) {
        this.beforeSnapshot = beforeSnapshot;
    }

    /**
     * 获取变更后快照。
     *
     * @return 变更后快照
     */
    public Object getAfterSnapshot() {
        return afterSnapshot;
    }

    /**
     * 设置变更后快照。
     *
     * @param afterSnapshot 变更后快照
     */
    public void setAfterSnapshot(Object afterSnapshot) {
        this.afterSnapshot = afterSnapshot;
    }
}
