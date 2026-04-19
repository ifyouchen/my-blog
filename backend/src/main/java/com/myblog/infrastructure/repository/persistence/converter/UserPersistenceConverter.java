package com.myblog.infrastructure.repository.persistence.converter;

import com.myblog.domain.model.aggregate.User;
import com.myblog.infrastructure.repository.persistence.entity.UserDO;
import com.myblog.shared.enums.UserRole;
import com.myblog.shared.enums.UserStatus;

/**
 * 用户持久化转换器。
 *
 * @author Codex
 * @since 1.0.0
 */
public final class UserPersistenceConverter {

    private UserPersistenceConverter() {
    }

    /**
     * 将用户数据对象转换为领域对象。
     *
     * @param userDO 用户数据对象
     * @return 用户领域对象
     */
    public static User toDomain(UserDO userDO) {
        if (userDO == null) {
            return null;
        }
        return User.restore(
            userDO.getId(),
            userDO.getUsername(),
            userDO.getEmail(),
            userDO.getPasswordHash(),
            userDO.getNickname(),
            userDO.getAvatarUrl(),
            userDO.getBio(),
            UserRole.valueOf(userDO.getRole()),
            UserStatus.valueOf(userDO.getStatus()),
            userDO.getCreatedAt(),
            userDO.getUpdatedAt()
        );
    }

    /**
     * 将用户领域对象转换为数据对象。
     *
     * @param user 用户领域对象
     * @return 用户数据对象
     */
    public static UserDO toData(User user) {
        UserDO userDO = new UserDO();
        userDO.setId(user.getId().getValue());
        userDO.setUsername(user.getUsername());
        userDO.setEmail(user.getEmail().getValue());
        userDO.setPasswordHash(user.getPasswordHash());
        userDO.setNickname(user.getNickname());
        userDO.setAvatarUrl(user.getAvatarUrl());
        userDO.setBio(user.getBio());
        userDO.setRole(user.getRole().name());
        userDO.setStatus(user.getStatus().name());
        userDO.setCreatedAt(user.getCreatedAt());
        userDO.setUpdatedAt(user.getUpdatedAt());
        return userDO;
    }
}
