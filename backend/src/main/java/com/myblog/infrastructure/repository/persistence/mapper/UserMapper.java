package com.myblog.infrastructure.repository.persistence.mapper;

import com.myblog.infrastructure.repository.persistence.entity.UserDO;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

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
     * 查询所有用户。
     *
     * @return 用户列表
     */
    List<UserDO> selectAll();

    /**
     * 分页查询后台用户列表。
     *
     * @param status 状态筛选
     * @param keyword 关键字
     * @param offset 偏移量
     * @param limit 限制数量
     * @return 用户列表
     */
    List<UserDO> selectAdminPage(@Param("status") String status,
                                 @Param("keyword") String keyword,
                                 @Param("offset") int offset,
                                 @Param("limit") int limit);

    /**
     * 统计后台用户数量。
     *
     * @param status 状态筛选
     * @param keyword 关键字
     * @return 用户数量
     */
    long countAdminPage(@Param("status") String status, @Param("keyword") String keyword);

    /**
     * 根据用户 ID 批量查询用户。
     *
     * @param userIds 用户 ID 列表
     * @return 用户列表
     */
    List<UserDO> selectByIds(@Param("userIds") List<Long> userIds);

    long countAll();

    long countByStatus(@Param("status") String status);

    long countCreatedOn(@Param("date") LocalDate date);

    long countCreatedSince(@Param("date") LocalDate date);

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
    int insertOrUpdate(UserDO userDO);


    /**
     * 更新用户。
     *
     * @param user 用户数据对象
     * @return 影响行数
     */
    int update(UserDO user);

    /**
     * 搜索用户。
     *
     * @param keyword 关键字
     * @param sort 排序方式
     * @param offset 偏移量
     * @param limit 限制数量
     * @return 用户列表
     */
    List<UserDO> searchUsers(@Param("keyword") String keyword,
                           @Param("sort") String sort,
                           @Param("offset") int offset,
                           @Param("limit") int limit);

    /**
     * 统计搜索用户数量。
     *
     * @param keyword 关键字
     * @return 用户数量
     */
    long countSearchUsers(@Param("keyword") String keyword);

    /**
     * 统计用户粉丝数。
     *
     * @param userId 用户ID
     * @return 粉丝数
     */
    int countFollowersByUserId(@Param("userId") Long userId);

    /**
     * 统计用户已发布文章数。
     *
     * @param userId 用户ID
     * @return 已发布文章数
     */
    int countPublishedArticlesByUserId(@Param("userId") Long userId);

    /**
     * 批量统计多个用户的粉丝数。
     *
     * @param userIds 用户ID列表
     * @return userId -> followerCount 映射列表
     */
    List<Map<String, Object>> countFollowersBatchByUserIds(@Param("userIds") List<Long> userIds);

    /**
     * 批量统计多个用户的已发布文章数。
     *
     * @param userIds 用户ID列表
     * @return userId -> articleCount 映射列表
     */
    List<Map<String, Object>> countPublishedArticlesBatchByUserIds(@Param("userIds") List<Long> userIds);
}
