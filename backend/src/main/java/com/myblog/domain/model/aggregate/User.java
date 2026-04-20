package com.myblog.domain.model.aggregate;

import com.myblog.domain.model.valueobject.Email;
import com.myblog.domain.model.valueobject.UserId;
import com.myblog.shared.enums.UserRole;
import com.myblog.shared.enums.UserStatus;
import com.myblog.shared.exception.DomainException;
import com.myblog.shared.exception.ErrorCode;

import java.time.LocalDateTime;

/**
 * 用户聚合根。
 *
 * @author Codex
 * @since 1.0.0
 */
public class User {

    private UserId id;
    private String username;
    private Email email;
    private String passwordHash;
    private String nickname;
    private String avatarUrl;
    private String bio;
    private UserRole role;
    private UserStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime deletedAt;

    private User() {
    }

    /**
     * 创建用户聚合根。
     *
     * @param id 用户 ID
     * @param username 用户名
     * @param email 邮箱
     * @param passwordHash 加密后的密码
     * @return 用户聚合根
     */
    public static User create(Long id, String username, String email, String passwordHash) {
        if (username == null || username.trim().length() < 3) {
            throw new DomainException(ErrorCode.PARAM_ERROR, "用户名至少3个字符");
        }
        if (passwordHash == null || passwordHash.trim().isEmpty()) {
            throw new DomainException(ErrorCode.PARAM_ERROR, "密码不能为空");
        }
        User user = new User();
        user.id = new UserId(id);
        user.username = username.trim();
        user.email = new Email(email);
        user.passwordHash = passwordHash;
        user.nickname = username.trim();
        user.avatarUrl = "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?auto=format&fit=crop&w=96&q=80";
        user.bio = "热爱技术写作";
        user.role = UserRole.USER;
        user.status = UserStatus.NORMAL;
        user.createdAt = LocalDateTime.now();
        user.updatedAt = user.createdAt;
        return user;
    }

    /**
     * 从持久化数据还原用户聚合根。
     *
     * @param id 用户 ID
     * @param username 用户名
     * @param email 邮箱
     * @param passwordHash 密码哈希
     * @param nickname 昵称
     * @param avatarUrl 头像地址
     * @param bio 个人简介
     * @param role 用户角色
     * @param status 用户状态
     * @param createdAt 创建时间
     * @param updatedAt 更新时间
     * @return 用户聚合根
     */
    public static User restore(Long id, String username, String email, String passwordHash, String nickname,
                               String avatarUrl, String bio, UserRole role, UserStatus status,
                               LocalDateTime createdAt, LocalDateTime updatedAt) {
        User user = new User();
        user.id = new UserId(id);
        user.username = username;
        user.email = new Email(email);
        user.passwordHash = passwordHash;
        user.nickname = nickname;
        user.avatarUrl = avatarUrl;
        user.bio = bio;
        user.role = role;
        user.status = status;
        user.createdAt = createdAt;
        user.updatedAt = updatedAt;
        return user;
    }

    /**
     * 校验用户是否允许登录。
     */
    public void ensureCanLogin() {
        if (!UserStatus.NORMAL.equals(status)) {
            throw new DomainException(ErrorCode.FORBIDDEN, "账号不可用");
        }
    }

    /**
     * 更新用户状态。
     */
    public void updateStatus(UserStatus status) {
        this.status = status;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 获取用户 ID。
     *
     * @return 用户 ID
     */
    public UserId getId() {
        return id;
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
     * 获取邮箱。
     *
     * @return 邮箱
     */
    public Email getEmail() {
        return email;
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
     * 获取用户昵称。
     *
     * @return 用户昵称
     */
    public String getNickname() {
        return nickname;
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
     * 获取个人简介。
     *
     * @return 个人简介
     */
    public String getBio() {
        return bio;
    }

    /**
     * 获取用户角色。
     *
     * @return 用户角色
     */
    public UserRole getRole() {
        return role;
    }

    /**
     * 获取用户状态。
     *
     * @return 用户状态
     */
    public UserStatus getStatus() {
        return status;
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
     * 获取更新时间。
     *
     * @return 更新时间
     */
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    /**
     * 判断是否已删除。
     */
    public boolean isDeleted() {
        return this.deletedAt != null;
    }
}
