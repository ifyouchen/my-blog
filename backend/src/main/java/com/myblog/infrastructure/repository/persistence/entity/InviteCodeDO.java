package com.myblog.infrastructure.repository.persistence.entity;

import java.time.LocalDateTime;

/**
 * 邀请码数据对象。
 *
 * @author Codex
 * @since 1.0.0
 */
public class InviteCodeDO {

    /**
     * 邀请码 ID
     */
    private Long id;

    /**
     * 邀请码字符串
     */
    private String code;

    /**
     * 创建者用户 ID
     */
    private Long creatorId;

    /**
     * 使用者用户 ID，未使用则为 null
     */
    private Long usedBy;

    /**
     * 使用时间，未使用则为 null
     */
    private LocalDateTime usedAt;

    /**
     * 邀请码过期时间
     */
    private LocalDateTime expiredAt;

    /**
     * 最大使用次数
     */
    private Integer maxUses;

    /**
     * 已使用次数
     */
    private Integer useCount;

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
     * 获取邀请码 ID。
     *
     * @return 邀请码 ID
     */
    public Long getId() {
        return id;
    }

    /**
     * 设置邀请码 ID。
     *
     * @param id 邀请码 ID
     */
    public void setId(Long id) {
        this.id = id;
    }

    /**
     * 获取邀请码字符串。
     *
     * @return 邀请码
     */
    public String getCode() {
        return code;
    }

    /**
     * 设置邀请码字符串。
     *
     * @param code 邀请码
     */
    public void setCode(String code) {
        this.code = code;
    }

    /**
     * 获取创建者用户 ID。
     *
     * @return 创建者用户 ID
     */
    public Long getCreatorId() {
        return creatorId;
    }

    /**
     * 设置创建者用户 ID。
     *
     * @param creatorId 创建者用户 ID
     */
    public void setCreatorId(Long creatorId) {
        this.creatorId = creatorId;
    }

    /**
     * 获取使用者用户 ID。
     *
     * @return 使用者用户 ID，未使用则为 null
     */
    public Long getUsedBy() {
        return usedBy;
    }

    /**
     * 设置使用者用户 ID。
     *
     * @param usedBy 使用者用户 ID
     */
    public void setUsedBy(Long usedBy) {
        this.usedBy = usedBy;
    }

    /**
     * 获取使用时间。
     *
     * @return 使用时间，未使用则为 null
     */
    public LocalDateTime getUsedAt() {
        return usedAt;
    }

    /**
     * 设置使用时间。
     *
     * @param usedAt 使用时间
     */
    public void setUsedAt(LocalDateTime usedAt) {
        this.usedAt = usedAt;
    }

    /**
     * 获取邀请码过期时间。
     *
     * @return 过期时间
     */
    public LocalDateTime getExpiredAt() {
        return expiredAt;
    }

    /**
     * 设置邀请码过期时间。
     *
     * @param expiredAt 过期时间
     */
    public void setExpiredAt(LocalDateTime expiredAt) {
        this.expiredAt = expiredAt;
    }

    /**
     * 获取最大使用次数。
     *
     * @return 最大使用次数
     */
    public Integer getMaxUses() {
        return maxUses;
    }

    /**
     * 设置最大使用次数。
     *
     * @param maxUses 最大使用次数
     */
    public void setMaxUses(Integer maxUses) {
        this.maxUses = maxUses;
    }

    /**
     * 获取已使用次数。
     *
     * @return 已使用次数
     */
    public Integer getUseCount() {
        return useCount;
    }

    /**
     * 设置已使用次数。
     *
     * @param useCount 已使用次数
     */
    public void setUseCount(Integer useCount) {
        this.useCount = useCount;
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

