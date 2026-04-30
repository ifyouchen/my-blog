package com.myblog.domain.model.aggregate;

import com.myblog.domain.model.valueobject.Email;
import com.myblog.domain.model.valueobject.UserId;
import com.myblog.shared.enums.UserRole;
import com.myblog.shared.enums.UserStatus;
import com.myblog.shared.exception.DomainException;
import com.myblog.shared.exception.ErrorCode;

import java.time.LocalDateTime;
import java.util.UUID;

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
    private String website;
    private String github;
    private String twitter;
    private String location;
    private String disableReason;
    private String passwordResetToken;
    private LocalDateTime passwordResetExpire;
    private LocalDateTime lastLoginAt;
    private String lastLoginIp;
    private LocalDateTime lastUsernameChangedAt;
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
     * @param website 个人网站
     * @param github GitHub 用户名
     * @param twitter Twitter/X 用户名
     * @param location 所在地
     * @param disableReason 禁用原因
     * @param passwordResetToken 密码重置 Token
     * @param passwordResetExpire 密码重置 Token 过期时间
     * @param lastLoginAt 最近登录时间
     * @param lastLoginIp 最近登录 IP
     * @param lastUsernameChangedAt 最近修改用户名时间
     * @param role 用户角色
     * @param status 用户状态
     * @param createdAt 创建时间
     * @param updatedAt 更新时间
     * @return 用户聚合根
     */
    public static User restore(Long id, String username, String email, String passwordHash, String nickname,
                               String avatarUrl, String bio, String website, String github, String twitter,
                               String location, String disableReason, String passwordResetToken,
                               LocalDateTime passwordResetExpire, LocalDateTime lastLoginAt, String lastLoginIp,
                               LocalDateTime lastUsernameChangedAt, UserRole role, UserStatus status,
                               LocalDateTime createdAt, LocalDateTime updatedAt) {
        User user = new User();
        user.id = new UserId(id);
        user.username = username;
        user.email = new Email(email);
        user.passwordHash = passwordHash;
        user.nickname = nickname;
        user.avatarUrl = avatarUrl;
        user.bio = bio;
        user.website = website;
        user.github = github;
        user.twitter = twitter;
        user.location = location;
        user.disableReason = disableReason;
        user.passwordResetToken = passwordResetToken;
        user.passwordResetExpire = passwordResetExpire;
        user.lastLoginAt = lastLoginAt;
        user.lastLoginIp = lastLoginIp;
        user.lastUsernameChangedAt = lastUsernameChangedAt;
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
        if (!UserStatus.DISABLED.equals(status)) {
            this.disableReason = null;
        }
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 禁用用户。
     *
     * @param reason 禁用原因
     */
    public void disable(String reason) {
        this.status = UserStatus.DISABLED;
        this.disableReason = normalizeNullableText(reason);
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 更新用户个人资料。
     *
     * @param nickname 昵称
     * @param avatarUrl 头像地址
     * @param bio 个人简介
     */
    public void updateProfile(String nickname, String avatarUrl, String bio) {
        if (nickname == null || nickname.trim().isEmpty()) {
            throw new DomainException(ErrorCode.PARAM_ERROR, "昵称不能为空");
        }
        this.nickname = nickname.trim();
        this.avatarUrl = avatarUrl == null ? null : avatarUrl.trim();
        this.bio = bio == null ? "" : bio.trim();
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 更新扩展资料（社交链接）。
     *
     * @param website 个人网站
     * @param github GitHub 用户名
     * @param twitter Twitter/X 用户名
     * @param location 所在地
     */
    public void updateExtendedProfile(String website, String github, String twitter, String location) {
        this.website = normalizeNullableText(website);
        this.github = normalizeNullableText(github);
        this.twitter = normalizeNullableText(twitter);
        this.location = normalizeNullableText(location);
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 绑定/更换邮箱。
     *
     * @param newEmail 新邮箱
     */
    public void changeEmail(String newEmail) {
        if (newEmail == null || newEmail.trim().isEmpty()) {
            throw new DomainException(ErrorCode.PARAM_ERROR, "邮箱不能为空");
        }
        this.email = new Email(newEmail.trim());
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 修改密码。
     *
     * @param newPasswordHash 新密码哈希
     */
    public void changePassword(String newPasswordHash) {
        if (newPasswordHash == null || newPasswordHash.trim().isEmpty()) {
            throw new DomainException(ErrorCode.PARAM_ERROR, "新密码不能为空");
        }
        this.passwordHash = newPasswordHash;
        this.passwordResetToken = null;
        this.passwordResetExpire = null;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 生成密码重置 Token（有效期 1 小时）。
     *
     * @return 重置 Token
     */
    public String generatePasswordResetToken() {
        this.passwordResetToken = UUID.randomUUID().toString().replace("-", "");
        this.passwordResetExpire = LocalDateTime.now().plusHours(1);
        this.updatedAt = LocalDateTime.now();
        return this.passwordResetToken;
    }

    /**
     * 校验密码重置 Token 是否有效。
     *
     * @param token 待校验的 Token
     */
    public void validatePasswordResetToken(String token) {
        if (this.passwordResetToken == null || !this.passwordResetToken.equals(token)) {
            throw new DomainException(ErrorCode.PARAM_ERROR, "重置链接无效或已过期");
        }
        if (this.passwordResetExpire == null || LocalDateTime.now().isAfter(this.passwordResetExpire)) {
            throw new DomainException(ErrorCode.PARAM_ERROR, "重置链接已过期，请重新申请");
        }
    }

    /**
     * 记录登录信息。
     *
     * @param ip 登录 IP
     */
    public void recordLogin(String ip) {
        this.lastLoginAt = LocalDateTime.now();
        this.lastLoginIp = ip;
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
     * 获取个人网站。
     *
     * @return 个人网站
     */
    public String getWebsite() {
        return website;
    }

    /**
     * 获取 GitHub 用户名。
     *
     * @return GitHub 用户名
     */
    public String getGithub() {
        return github;
    }

    /**
     * 获取 Twitter/X 用户名。
     *
     * @return Twitter/X 用户名
     */
    public String getTwitter() {
        return twitter;
    }

    /**
     * 获取所在地。
     *
     * @return 所在地
     */
    public String getLocation() {
        return location;
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
     * 获取密码重置 Token。
     *
     * @return 密码重置 Token
     */
    public String getPasswordResetToken() {
        return passwordResetToken;
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
     * 获取最近登录时间。
     *
     * @return 最近登录时间
     */
    public LocalDateTime getLastLoginAt() {
        return lastLoginAt;
    }

    /**
     * 获取最近登录 IP。
     *
     * @return 最近登录 IP
     */
    public String getLastLoginIp() {
        return lastLoginIp;
    }

    /**
     * 获取最近修改用户名时间。
     *
     * @return 最近修改用户名时间
     */
    public LocalDateTime getLastUsernameChangedAt() {
        return lastUsernameChangedAt;
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

    private static String normalizeNullableText(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
