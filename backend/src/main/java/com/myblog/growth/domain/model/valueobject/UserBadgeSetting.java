package com.myblog.growth.domain.model.valueobject;

import java.time.LocalDateTime;

/**
 * 用户徽章佩戴设置.
 *
 * @author Codex
 * @since 1.0.0
 */
public class UserBadgeSetting {

    private Long id;
    private Long userId;
    private String equippedBadgeCode;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private int version;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getEquippedBadgeCode() { return equippedBadgeCode; }
    public void setEquippedBadgeCode(String equippedBadgeCode) { this.equippedBadgeCode = equippedBadgeCode; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    public int getVersion() { return version; }
    public void setVersion(int version) { this.version = version; }
}
