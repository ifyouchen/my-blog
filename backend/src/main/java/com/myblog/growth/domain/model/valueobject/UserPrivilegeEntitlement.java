package com.myblog.growth.domain.model.valueobject;

import java.time.LocalDateTime;

/**
 * 用户权益授予记录值对象.
 *
 * @author Codex
 * @since 2026-05-17
 */
public class UserPrivilegeEntitlement {

    private Long id;
    private Long userId;
    private String privilegeCode;
    private int sourceLevel;
    private String status;
    private LocalDateTime grantedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private int version;

    /**
     * 创建新的权益授予记录.
     *
     * @param userId        用户ID
     * @param privilegeCode 权益编码
     * @param sourceLevel   来源等级
     * @return 授予记录
     */
    public static UserPrivilegeEntitlement grant(Long userId, String privilegeCode, int sourceLevel) {
        UserPrivilegeEntitlement entitlement = new UserPrivilegeEntitlement();
        entitlement.userId = userId;
        entitlement.privilegeCode = privilegeCode;
        entitlement.sourceLevel = sourceLevel;
        entitlement.status = "ACTIVE";
        entitlement.grantedAt = LocalDateTime.now();
        return entitlement;
    }

    /**
     * 获取主键ID.
     *
     * @return 主键ID
     */
    public Long getId() {
        return id;
    }

    /**
     * 设置主键ID.
     *
     * @param id 主键ID
     */
    public void setId(Long id) {
        this.id = id;
    }

    /**
     * 获取用户ID.
     *
     * @return 用户ID
     */
    public Long getUserId() {
        return userId;
    }

    /**
     * 设置用户ID.
     *
     * @param userId 用户ID
     */
    public void setUserId(Long userId) {
        this.userId = userId;
    }

    /**
     * 获取权益编码.
     *
     * @return 权益编码
     */
    public String getPrivilegeCode() {
        return privilegeCode;
    }

    /**
     * 设置权益编码.
     *
     * @param privilegeCode 权益编码
     */
    public void setPrivilegeCode(String privilegeCode) {
        this.privilegeCode = privilegeCode;
    }

    /**
     * 获取来源等级.
     *
     * @return 来源等级
     */
    public int getSourceLevel() {
        return sourceLevel;
    }

    /**
     * 设置来源等级.
     *
     * @param sourceLevel 来源等级
     */
    public void setSourceLevel(int sourceLevel) {
        this.sourceLevel = sourceLevel;
    }

    /**
     * 获取状态.
     *
     * @return 状态
     */
    public String getStatus() {
        return status;
    }

    /**
     * 设置状态.
     *
     * @param status 状态
     */
    public void setStatus(String status) {
        this.status = status;
    }

    /**
     * 获取发放时间.
     *
     * @return 发放时间
     */
    public LocalDateTime getGrantedAt() {
        return grantedAt;
    }

    /**
     * 设置发放时间.
     *
     * @param grantedAt 发放时间
     */
    public void setGrantedAt(LocalDateTime grantedAt) {
        this.grantedAt = grantedAt;
    }

    /**
     * 获取创建时间.
     *
     * @return 创建时间
     */
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    /**
     * 设置创建时间.
     *
     * @param createdAt 创建时间
     */
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    /**
     * 获取更新时间.
     *
     * @return 更新时间
     */
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    /**
     * 设置更新时间.
     *
     * @param updatedAt 更新时间
     */
    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    /**
     * 获取版本号.
     *
     * @return 版本号
     */
    public int getVersion() {
        return version;
    }

    /**
     * 设置版本号.
     *
     * @param version 版本号
     */
    public void setVersion(int version) {
        this.version = version;
    }
}
