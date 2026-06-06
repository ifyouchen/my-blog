package com.myblog.domain.model.readmodel;

import java.time.LocalDateTime;

/**
 * 用户搜索历史。
 *
 * @author Codex
 * @since 1.0.0
 */
public class UserSearchHistory {

    private Long id;
    private Long userId;
    private String keyword;
    private Integer searchCount;
    private LocalDateTime lastSearchedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime deletedAt;
    private Long version;

    /**
     * 获取记录 ID。
     *
     * @return 记录 ID
     */
    public Long getId() {
        return id;
    }

    /**
     * 设置记录 ID。
     *
     * @param id 记录 ID
     */
    public void setId(Long id) {
        this.id = id;
    }

    /**
     * 获取用户 ID。
     *
     * @return 用户 ID
     */
    public Long getUserId() {
        return userId;
    }

    /**
     * 设置用户 ID。
     *
     * @param userId 用户 ID
     */
    public void setUserId(Long userId) {
        this.userId = userId;
    }

    /**
     * 获取关键词。
     *
     * @return 关键词
     */
    public String getKeyword() {
        return keyword;
    }

    /**
     * 设置关键词。
     *
     * @param keyword 关键词
     */
    public void setKeyword(String keyword) {
        this.keyword = keyword;
    }

    /**
     * 获取搜索次数。
     *
     * @return 搜索次数
     */
    public Integer getSearchCount() {
        return searchCount;
    }

    /**
     * 设置搜索次数。
     *
     * @param searchCount 搜索次数
     */
    public void setSearchCount(Integer searchCount) {
        this.searchCount = searchCount;
    }

    /**
     * 获取最后搜索时间。
     *
     * @return 最后搜索时间
     */
    public LocalDateTime getLastSearchedAt() {
        return lastSearchedAt;
    }

    /**
     * 设置最后搜索时间。
     *
     * @param lastSearchedAt 最后搜索时间
     */
    public void setLastSearchedAt(LocalDateTime lastSearchedAt) {
        this.lastSearchedAt = lastSearchedAt;
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

    /**
     * 获取删除时间。
     *
     * @return 删除时间
     */
    public LocalDateTime getDeletedAt() {
        return deletedAt;
    }

    /**
     * 设置删除时间。
     *
     * @param deletedAt 删除时间
     */
    public void setDeletedAt(LocalDateTime deletedAt) {
        this.deletedAt = deletedAt;
    }

    /**
     * 获取版本号。
     *
     * @return 版本号
     */
    public Long getVersion() {
        return version;
    }

    /**
     * 设置版本号。
     *
     * @param version 版本号
     */
    public void setVersion(Long version) {
        this.version = version;
    }
}
