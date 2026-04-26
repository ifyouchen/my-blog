package com.myblog.infrastructure.repository.persistence.repository;

import com.myblog.domain.model.aggregate.User;
import com.myblog.domain.model.valueobject.UserId;
import com.myblog.domain.repository.UserRepository;
import com.myblog.infrastructure.repository.persistence.converter.UserPersistenceConverter;
import com.myblog.infrastructure.repository.persistence.entity.UserDO;
import com.myblog.infrastructure.repository.persistence.mapper.UserMapper;
import com.myblog.shared.enums.UserStatus;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
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
     * 查询所有用户。
     */
    @Override
    public List<User> findAll() {
        List<UserDO> userDOList = userMapper.selectAll();
        List<User> users = new ArrayList<>(userDOList.size());
        for (UserDO userDO : userDOList) {
            users.add(UserPersistenceConverter.toDomain(userDO));
        }
        return users;
    }

    /**
     * 分页查询后台用户列表。
     *
     * @param status 状态筛选
     * @param keyword 关键字
     * @param page 页码
     * @param pageSize 每页大小
     * @return 用户列表
     */
    @Override
    public List<User> findAdminPage(String status, String keyword, int page, int pageSize) {
        int currentPage = Math.max(page, 1);
        int currentPageSize = Math.max(pageSize, 1);
        int offset = (currentPage - 1) * currentPageSize;
        List<UserDO> userDOList = userMapper.selectAdminPage(status, keyword, offset, currentPageSize);
        List<User> users = new ArrayList<User>(userDOList.size());
        for (UserDO userDO : userDOList) {
            users.add(UserPersistenceConverter.toDomain(userDO));
        }
        return users;
    }

    /**
     * 统计后台用户数量。
     *
     * @param status 状态筛选
     * @param keyword 关键字
     * @return 用户数量
     */
    @Override
    public long countAdminPage(String status, String keyword) {
        return userMapper.countAdminPage(status, keyword);
    }

    /**
     * 根据用户 ID 批量查询用户。
     *
     * @param userIds 用户 ID 列表
     * @return 用户列表
     */
    @Override
    public List<User> findByIds(List<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return new ArrayList<User>();
        }
        List<UserDO> userDOList = userMapper.selectByIds(userIds);
        List<User> users = new ArrayList<User>(userDOList.size());
        for (UserDO userDO : userDOList) {
            users.add(UserPersistenceConverter.toDomain(userDO));
        }
        return users;
    }

    @Override
    public long countAll() {
        return userMapper.countAll();
    }

    @Override
    public long countByStatus(UserStatus status) {
        return userMapper.countByStatus(status.name());
    }

    @Override
    public long countCreatedOn(LocalDate date) {
        return userMapper.countCreatedOn(date);
    }

    @Override
    public long countCreatedSince(LocalDate date) {
        return userMapper.countCreatedSince(date);
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

    @Override
    public List<User> searchUsers(String keyword, String sort, int page, int pageSize) {
        int offset = (Math.max(page, 1) - 1) * Math.max(pageSize, 1);
        List<UserDO> userDOList = userMapper.searchUsers(keyword, sort, offset, pageSize);
        List<User> users = new ArrayList<>(userDOList.size());
        for (UserDO userDO : userDOList) {
            users.add(UserPersistenceConverter.toDomain(userDO));
        }
        return users;
    }

    @Override
    public long countSearchUsers(String keyword) {
        return userMapper.countSearchUsers(keyword);
    }

    @Override
    public int countFollowers(Long userId) {
        return userMapper.countFollowersByUserId(userId);
    }

    @Override
    public int countPublishedArticles(Long userId) {
        return userMapper.countPublishedArticlesByUserId(userId);
    }
}
