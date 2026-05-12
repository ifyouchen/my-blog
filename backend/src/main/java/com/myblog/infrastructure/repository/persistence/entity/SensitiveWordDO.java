package com.myblog.infrastructure.repository.persistence.entity;

import java.time.LocalDateTime;

/**
 * 敏感词持久化对象。
 *
 * @author Codex
 * @since 1.0.0
 */
public class SensitiveWordDO {

    /**
     * 敏感词 ID
     */
    private Long id;

    /**
     * 敏感词内容
     */
    private String word;

    /**
     * 敏感词分类（如：政治、色情、广告）
     */
    private String category;

    /**
     * 敏感词级别（数字越大危害越高）
     */
    private Integer level;

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
     * 获取敏感词 ID。
     *
     * @return 敏感词 ID
     */
    public Long getId() {
        return id;
    }

    /**
     * 设置敏感词 ID。
     *
     * @param id 敏感词 ID
     */
    public void setId(Long id) {
        this.id = id;
    }

    /**
     * 获取敏感词内容。
     *
     * @return 敏感词内容
     */
    public String getWord() {
        return word;
    }

    /**
     * 设置敏感词内容。
     *
     * @param word 敏感词内容
     */
    public void setWord(String word) {
        this.word = word;
    }

    /**
     * 获取敏感词分类。
     *
     * @return 分类
     */
    public String getCategory() {
        return category;
    }

    /**
     * 设置敏感词分类。
     *
     * @param category 分类
     */
    public void setCategory(String category) {
        this.category = category;
    }

    /**
     * 获取敏感词级别。
     *
     * @return 级别
     */
    public Integer getLevel() {
        return level;
    }

    /**
     * 设置敏感词级别。
     *
     * @param level 级别
     */
    public void setLevel(Integer level) {
        this.level = level;
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

