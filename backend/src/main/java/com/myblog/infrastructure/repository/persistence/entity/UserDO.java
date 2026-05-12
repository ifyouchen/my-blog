package com.myblog.infrastructure.repository.persistence.entity;

import java.time.LocalDateTime;

/**
 * 用户数据对象。
 *
 * @author Codex
 * @since 1.0.0
 */
public class UserDO {

    /**
     * 用户 ID
     */
    private Long id;

    /**
     * 用户名（登录账号）
     */
    private String username;

    /**
     * 邮箱
     */
    private String email;

    /**
     * 密码哈希值（BCrypt）
     */
    private String passwordHash;

    /**
     * 昵称
     */
    private String nickname;

    /**
     * 头像图片地址
     */
    private String avatarUrl;

    /**
     * 个人简介
     */
    private String bio;

    /**
     * 个人网站地址
     */
    private String website;

    /**
     * GitHub 主页地址
     */
    private String github;

    /**
     * Twitter 主页地址
     */
    private String twitter;

    /**
     * 所在地区
     */
    private String location;

    /**
     * 账号禁用原因
     */
    private String disableReason;

    /**
     * 密码重置 Token
     */
    private String passwordResetToken;

    /**
     * 密码重置 Token 过期时间
     */
    private LocalDateTime passwordResetExpire;

    /**
     * 最后登录时间
     */
    private LocalDateTime lastLoginAt;

    /**
     * 最后登录 IP 地址
     */
    private String lastLoginIp;

    /**
     * 最后修改用户名的时间
     */
    private LocalDateTime lastUsernameChangedAt;

    /**
     * 用户角色（USER / ADMIN）
     */
    private String role;

    /**
     * 用户状态（NORMAL / DISABLED / DELETED）
     */
    private String status;

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
     * 是否推荐展示（1 是 / 0 否）
     */
    private Integer recommended;

    /**
     * 推荐权重，值越大越靠前
     */
    private Integer recommendWeight;

    /**
     * 获取用户 ID。
     *
     * @return 用户 ID
     */
    public Long getId() {
        return id;
    }

    /**
     * 设置用户 ID。
     *
     * @param id 用户 ID
     */
    public void setId(Long id) {
        this.id = id;
    }

    /**
     * 获取用户名。
     *
     * @return 用户名
     */
    public String getUsername() {
        return username;
    }

    /**
     * 设置用户名。
     *
     * @param username 用户名
     */
    public void setUsername(String username) {
        this.username = username;
    }

    /**
     * 获取邮箱。
     *
     * @return 邮箱
     */
    public String getEmail() {
        return email;
    }

    /**
     * 设置邮箱。
     *
     * @param email 邮箱
     */
    public void setEmail(String email) {
        this.email = email;
    }

    /**
     * 获取密码哈希值。
     *
     * @return 密码哈希值
     */
    public String getPasswordHash() {
        return passwordHash;
    }

    /**
     * 设置密码哈希值。
     *
     * @param passwordHash 密码哈希值
     */
    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    /**
     * 获取昵称。
     *
     * @return 昵称
     */
    public String getNickname() {
        return nickname;
    }

    /**
     * 设置昵称。
     *
     * @param nickname 昵称
     */
    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    /**
     * 获取头像图片地址。
     *
     * @return 头像图片地址
     */
    public String getAvatarUrl() {
        return avatarUrl;
    }

    /**
     * 设置头像图片地址。
     *
     * @param avatarUrl 头像图片地址
     */
    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }

    /**
     * 获取个人简介。
     *
     * @return 个人简介
     */
    public String getBio() {
        return bio;
    }

    /**
     * 设置个人简介。
     *
     * @param bio 个人简介
     */
    public void setBio(String bio) {
        this.bio = bio;
    }

    /**
     * 获取个人网站地址。
     *
     * @return 个人网站地址
     */
    public String getWebsite() {
        return website;
    }

    /**
     * 设置个人网站地址。
     *
     * @param website 个人网站地址
     */
    public void setWebsite(String website) {
        this.website = website;
    }

    /**
     * 获取 GitHub 主页地址。
     *
     * @return GitHub 主页地址
     */
    public String getGithub() {
        return github;
    }

    /**
     * 设置 GitHub 主页地址。
     *
     * @param github GitHub 主页地址
     */
    public void setGithub(String github) {
        this.github = github;
    }

    /**
     * 获取 Twitter 主页地址。
     *
     * @return Twitter 主页地址
     */
    public String getTwitter() {
        return twitter;
    }

    /**
     * 设置 Twitter 主页地址。
     *
     * @param twitter Twitter 主页地址
     */
    public void setTwitter(String twitter) {
        this.twitter = twitter;
    }

    /**
     * 获取所在地区。
     *
     * @return 所在地区
     */
    public String getLocation() {
        return location;
    }

    /**
     * 设置所在地区。
     *
     * @param location 所在地区
     */
    public void setLocation(String location) {
        this.location = location;
    }

    /**
     * 获取密码重置 Token。
     *
     * @return 密码重置 Token
     */
    public String getPasswordResetToken() {
        return passwordResetToken;
    }

    /**
     * 设置密码重置 Token。
     *
     * @param passwordResetToken 密码重置 Token
     */
    public void setPasswordResetToken(String passwordResetToken) {
        this.passwordResetToken = passwordResetToken;
    }

    /**
     * 获取密码重置 Token 过期时间。
     *
     * @return 过期时间
     */
    public LocalDateTime getPasswordResetExpire() {
        return passwordResetExpire;
    }

    /**
     * 设置密码重置 Token 过期时间。
     *
     * @param passwordResetExpire 过期时间
     */
    public void setPasswordResetExpire(LocalDateTime passwordResetExpire) {
        this.passwordResetExpire = passwordResetExpire;
    }

    /**
     * 获取最后登录时间。
     *
     * @return 最后登录时间
     */
    public LocalDateTime getLastLoginAt() {
        return lastLoginAt;
    }

    /**
     * 设置最后登录时间。
     *
     * @param lastLoginAt 最后登录时间
     */
    public void setLastLoginAt(LocalDateTime lastLoginAt) {
        this.lastLoginAt = lastLoginAt;
    }

    /**
     * 获取最后登录 IP 地址。
     *
     * @return 最后登录 IP 地址
     */
    public String getLastLoginIp() {
        return lastLoginIp;
    }

    /**
     * 设置最后登录 IP 地址。
     *
     * @param lastLoginIp 最后登录 IP 地址
     */
    public void setLastLoginIp(String lastLoginIp) {
        this.lastLoginIp = lastLoginIp;
    }

    /**
     * 获取最后修改用户名的时间。
     *
     * @return 最后修改用户名时间
     */
    public LocalDateTime getLastUsernameChangedAt() {
        return lastUsernameChangedAt;
    }

    /**
     * 设置最后修改用户名的时间。
     *
     * @param lastUsernameChangedAt 最后修改用户名时间
     */
    public void setLastUsernameChangedAt(LocalDateTime lastUsernameChangedAt) {
        this.lastUsernameChangedAt = lastUsernameChangedAt;
    }

    /**
     * 获取账号禁用原因。
     *
     * @return 禁用原因
     */
    public String getDisableReason() {
        return disableReason;
    }

    /**
     * 设置账号禁用原因。
     *
     * @param disableReason 禁用原因
     */
    public void setDisableReason(String disableReason) {
        this.disableReason = disableReason;
    }

    /**
     * 获取用户角色。
     *
     * @return 用户角色
     */
    public String getRole() {
        return role;
    }

    /**
     * 设置用户角色。
     *
     * @param role 用户角色
     */
    public void setRole(String role) {
        this.role = role;
    }

    /**
     * 获取用户状态。
     *
     * @return 用户状态
     */
    public String getStatus() {
        return status;
    }

    /**
     * 设置用户状态。
     *
     * @param status 用户状态
     */
    public void setStatus(String status) {
        this.status = status;
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
     * @return 删除时间
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

    /**
     * 获取是否推荐展示（1 是 / 0 否）。
     *
     * @return 是否推荐
     */
    public Integer getRecommended() {
        return recommended;
    }

    /**
     * 设置是否推荐展示。
     *
     * @param recommended 是否推荐（1 是 / 0 否）
     */
    public void setRecommended(Integer recommended) {
        this.recommended = recommended;
    }

    /**
     * 获取推荐权重。
     *
     * @return 推荐权重
     */
    public Integer getRecommendWeight() {
        return recommendWeight;
    }

    /**
     * 设置推荐权重。
     *
     * @param recommendWeight 推荐权重
     */
    public void setRecommendWeight(Integer recommendWeight) {
        this.recommendWeight = recommendWeight;
    }
}
