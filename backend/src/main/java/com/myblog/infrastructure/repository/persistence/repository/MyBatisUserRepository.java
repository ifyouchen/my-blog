package com.myblog.infrastructure.repository.persistence.repository;

import com.myblog.domain.model.aggregate.User;
import com.myblog.domain.model.valueobject.UserId;
import com.myblog.domain.repository.UserRepository;
import com.myblog.infrastructure.repository.persistence.converter.UserPersistenceConverter;
import com.myblog.infrastructure.repository.persistence.entity.UserDO;
import com.myblog.infrastructure.repository.persistence.mapper.UserMapper;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * 用户 MyBatis 仓储实现。
 *
 * @author Codex
 * @since 1.0.0
 */
@Repository
@Profile("!memory")
public class MyBatisUserRepository implements UserRepository {

    private final UserMapper userMapper;

    /**
     * 创建用户 MyBatis 仓储。
     *
     * @param userMapper 用户 Mapper
     */
    public MyBatisUserRepository(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    /**
     * 根据用户 ID 查询用户。
     *
     * @param userId 用户 ID
     * @return 用户 Optional
     */
    @Override
    public Optional<User> findById(UserId userId) {
        return Optional.ofNullable(UserPersistenceConverter.toDomain(userMapper.selectById(userId.getValue())));
    }

    /**
     * 根据账号查询用户。
     *
     * @param account 用户名或邮箱
     * @return 用户 Optional
     */
    @Override
    public Optional<User> findByAccount(String account) {
        if (account == null || account.trim().length() == 0) {
            return Optional.empty();
        }
        UserDO userDO = userMapper.selectByAccount(account.trim());
        return Optional.ofNullable(UserPersistenceConverter.toDomain(userDO));
    }

    /**
     * 判断用户名是否存在。
     *
     * @param username 用户名
     * @return 是否存在
     */
    @Override
    public boolean existsByUsername(String username) {
        return userMapper.countByUsername(username) > 0;
    }

    /**
     * 判断邮箱是否存在。
     *
     * @param email 邮箱
     * @return 是否存在
     */
    @Override
    public boolean existsByEmail(String email) {
        return userMapper.countByEmail(email) > 0;
    }

    /**
     * 保存用户。
     *
     * @param user 用户聚合根
     * @return 保存后的用户
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public User save(User user) {
        UserDO userDO = UserPersistenceConverter.toData(user);
        if (userMapper.countById(user.getId().getValue()) > 0) {
            userMapper.update(userDO);
            return user;
        }
        userMapper.insert(userDO);
        return user;
    }

    /**
     * 生成下一个用户 ID。
     *
     * @return 用户 ID
     */
    @Override
    public Long nextId() {
        Long nextId = userMapper.selectNextId();
        return nextId == null ? 1001L : nextId;
    }
}
