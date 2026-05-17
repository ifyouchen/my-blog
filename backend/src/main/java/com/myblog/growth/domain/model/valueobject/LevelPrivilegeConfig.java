package com.myblog.growth.domain.model.valueobject;

import java.time.LocalDateTime;

/**
 * 等级权益配置值对象.
 *
 * @author Codex
 * @since 2026-05-17
 */
public class LevelPrivilegeConfig {

    private Long id;
    private int level;
    private String privilegeCode;
    private String privilegeName;
    private String description;
    private boolean enabled;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private int version;

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
     * 获取等级.
     *
     * @return 等级
     */
    public int getLevel() {
        return level;
    }

    /**
     * 设置等级.
     *
     * @param level 等级
     */
    public void setLevel(int level) {
        this.level = level;
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
     * 获取权益名称.
     *
     * @return 权益名称
     */
    public String getPrivilegeName() {
        return privilegeName;
    }

    /**
     * 设置权益名称.
     *
     * @param privilegeName 权益名称
     */
    public void setPrivilegeName(String privilegeName) {
        this.privilegeName = privilegeName;
    }

    /**
     * 获取权益说明.
     *
     * @return 权益说明
     */
    public String getDescription() {
        return description;
    }

    /**
     * 设置权益说明.
     *
     * @param description 权益说明
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * 是否启用.
     *
     * @return true 表示启用
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * 设置是否启用.
     *
     * @param enabled 是否启用
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
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
