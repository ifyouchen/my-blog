package com.myblog.growth.domain.model.valueobject;

import java.time.LocalDateTime;

/**
 * 用户已拥有徽章值对象.
 *
 * @author Codex
 * @since 1.0.0
 */
public class UserBadge {

    private Long id;
    private Long userId;
    private String badgeCode;
    private String sourceType;
    private Long sourceId;
    private LocalDateTime grantedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private int version;

    public static UserBadge grant(Long userId, String badgeCode, String sourceType, Long sourceId) {
        UserBadge badge = new UserBadge();
        badge.setUserId(userId);
        badge.setBadgeCode(badgeCode);
        badge.setSourceType(sourceType);
        badge.setSourceId(sourceId);
        return badge;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getBadgeCode() { return badgeCode; }
    public void setBadgeCode(String badgeCode) { this.badgeCode = badgeCode; }
    public String getSourceType() { return sourceType; }
    public void setSourceType(String sourceType) { this.sourceType = sourceType; }
    public Long getSourceId() { return sourceId; }
    public void setSourceId(Long sourceId) { this.sourceId = sourceId; }
    public LocalDateTime getGrantedAt() { return grantedAt; }
    public void setGrantedAt(LocalDateTime grantedAt) { this.grantedAt = grantedAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    public int getVersion() { return version; }
    public void setVersion(int version) { this.version = version; }
}
