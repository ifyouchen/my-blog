package com.myblog.infrastructure.repository.persistence.mapper;

import com.myblog.infrastructure.repository.persistence.entity.UserDO;
import org.apache.ibatis.annotations.Param;

/**
 * 用户 MyBatis Mapper。
 *
 * @author Codex
 * @since 1.0.0
 */
public interface UserMapper {

    /**
     * 根据 ID 查询用户。
     *
     * @param id 用户 ID
     * @return 用户数据对象
     */
    UserDO selectById(@Param("id") Long id);

    /**
     * 根据账号查询用户。
     *
     * @param account 用户名或邮箱
     * @return 用户数据对象
     */
    UserDO selectByAccount(@Param("account") String account);

    /**
     * 根据用户名统计用户数量。
     *
     * @param username 用户名
     * @return 用户数量
     */
    int countByUsername(@Param("username") String username);

    /**
     * 根据邮箱统计用户数量。
     *
     * @param email 邮箱
     * @return 用户数量
     */
    int countByEmail(@Param("email") String email);

    /**
     * 根据 ID 统计用户数量。
     *
     * @param id 用户 ID
     * @return 用户数量
     */
    int countById(@Param("id") Long id);

    /**
     * 查询下一个用户 ID。
     *
     * @return 下一个用户 ID
     */
    Long selectNextId();

    /**
     * 插入用户。
     *
     * @param user 用户数据对象
     * @return 影响行数
     */
    int insert(UserDO user);

    /**
     * 更新用户。
     *
     * @param user 用户数据对象
     * @return 影响行数
     */
    int update(UserDO user);
}
