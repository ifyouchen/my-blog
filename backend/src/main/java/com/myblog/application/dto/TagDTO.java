package com.myblog.application.dto;

import java.time.LocalDateTime;

/**
 * 标签数据传输对象。
 *
 * @author my-blog
 * @since 1.0.0
 */
public class TagDTO {

    /** 标签 ID */
    private Long id;
    /** 标签名称 */
    private String name;
    /** 标签描述 */
    private String description;
    /** 所属大类 */
    private String groupName;
    /** 是否启用 */
    private Boolean enabled;
    /** 创建时间 */
    private LocalDateTime createdAt;
    /** 更新时间 */
    private LocalDateTime updatedAt;

    /**
     * 获取标签 ID。
     *
     * @return 标签 ID
     */
    public Long getId() {
        return id;
    }

    /**
     * 设置标签 ID。
     *
     * @param id 标签 ID
     */
    public void setId(Long id) {
        this.id = id;
    }

    /**
     * 获取标签名称。
     *
     * @return 标签名称
     */
    public String getName() {
        return name;
    }

    /**
     * 设置标签名称。
     *
     * @param name 标签名称
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * 获取标签描述。
     *
     * @return 标签描述
     */
    public String getDescription() {
        return description;
    }

    /**
     * 设置标签描述。
     *
     * @param description 标签描述
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * 获取所属大类。
     *
     * @return 所属大类
     */
    public String getGroupName() {
        return groupName;
    }

    /**
     * 设置所属大类。
     *
     * @param groupName 所属大类
     */
    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }

    /**
     * 获取是否启用。
     *
     * @return 是否启用
     */
    public Boolean getEnabled() {
        return enabled;
    }

    /**
     * 设置是否启用。
     *
     * @param enabled 是否启用
     */
    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * 获取创建时间。
     *
     * @return 创建时间
     */
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    /**
     * 设置创建时间。
     *
     * @param createdAt 创建时间
     */
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    /**
     * 获取更新时间。
     *
     * @return 更新时间
     */
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    /**
     * 设置更新时间。
     *
     * @param updatedAt 更新时间
     */
    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}