package com.myblog.infrastructure.repository.persistence.entity;

import java.time.LocalDateTime;

/**
 * 用户数据对象。
 *
 * @author Codex
 * @since 1.0.0
 */
public class UserDO {

    private Long id;
    private String username;
    private String email;
    private String passwordHash;
    private String nickname;
    private String avatarUrl;
    private String bio;
    private String disableReason;
    private String role;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime deletedAt;
    private Integer version;

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
     * 获取密码哈希。
     *
     * @return 密码哈希
     */
    public String getPasswordHash() {
        return passwordHash;
    }

    /**
     * 设置密码哈希。
     *
     * @param passwordHash 密码哈希
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
     * 获取头像地址。
     *
     * @return 头像地址
     */
    public String getAvatarUrl() {
        return avatarUrl;
    }

    /**
     * 设置头像地址。
     *
     * @param avatarUrl 头像地址
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
     * 获取禁用原因。
     *
     * @return 禁用原因
     */
    public String getDisableReason() {
        return disableReason;
    }

    /**
     * 设置禁用原因。
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
    public Integer getVersion() {
        return version;
    }

    /**
     * 设置版本号。
     *
     * @param version 版本号
     */
    public void setVersion(Integer version) {
        this.version = version;
    }
}
