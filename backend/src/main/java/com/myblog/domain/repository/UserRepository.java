package com.myblog.domain.repository;

import com.myblog.domain.model.aggregate.User;
import com.myblog.domain.model.valueobject.UserId;

import java.util.Optional;

/**
 * 用户仓储接口。
 *
 * @author Codex
 * @since 1.0.0
 */
public interface UserRepository {

    /**
     * 根据用户 ID 查询用户。
     *
     * @param userId 用户 ID
     * @return 用户 Optional
     */
    Optional<User> findById(UserId userId);

    /**
     * 根据账号查询用户。
     *
     * @param account 用户名或邮箱
     * @return 用户 Optional
     */
    Optional<User> findByAccount(String account);

    /**
     * 判断用户名是否已存在。
     *
     * @param username 用户名
     * @return 是否存在
     */
    boolean existsByUsername(String username);

    /**
     * 判断邮箱是否已存在。
     *
     * @param email 邮箱
     * @return 是否存在
     */
    boolean existsByEmail(String email);

    /**
     * 保存用户。
     *
     * @param user 用户聚合根
     * @return 保存后的用户
     */
    User save(User user);

    /**
     * 生成下一个用户 ID。
     *
     * @return 用户 ID
     */
    Long nextId();
}
