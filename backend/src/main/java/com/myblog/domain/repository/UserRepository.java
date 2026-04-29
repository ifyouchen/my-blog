package com.myblog.domain.repository;

import com.myblog.domain.model.aggregate.User;
import com.myblog.domain.model.valueobject.UserId;
import com.myblog.shared.enums.UserStatus;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
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
     * 分页查询后台用户列表。
     *
     * @param status 状态筛选
     * @param keyword 关键字
     * @param page 页码
     * @param pageSize 每页大小
     * @return 用户列表
     */
    List<User> findAdminPage(String status, String keyword, int page, int pageSize);

    /**
     * 统计后台用户数量。
     *
     * @param status 状态筛选
     * @param keyword 关键字
     * @return 用户数量
     */
    long countAdminPage(String status, String keyword);

    /**
     * 根据用户 ID 批量查询用户。
     *
     * @param userIds 用户 ID 列表
     * @return 用户列表
     */
    List<User> findByIds(List<Long> userIds);

    /**
     * 统计用户数量。
     *
     * @return 用户数量
     */
    long countAll();

    /**
     * 按状态统计用户数量。
     *
     * @param status 用户状态
     * @return 用户数量
     */
    long countByStatus(UserStatus status);

    /**
     * 统计指定日期创建的用户数量。
     *
     * @param date 日期
     * @return 用户数量
     */
    long countCreatedOn(LocalDate date);

    /**
     * 统计指定日期之后创建的用户数量。
     *
     * @param date 起始日期
     * @return 用户数量
     */
    long countCreatedSince(LocalDate date);

    /**
     * 查询所有用户。
     *
     * @return 用户列表
     */
    List<User> findAll();

    /**
     * 生成下一个用户 ID。
     *
     * @return 用户 ID
     */
    Long nextId();

    /**
     * 搜索用户。
     *
     * @param keyword 关键字
     * @param sort 排序方式
     * @param page 页码
     * @param pageSize 每页大小
     * @return 用户列表
     */
    List<User> searchUsers(String keyword, String sort, int page, int pageSize);

    /**
     * 统计搜索用户数量。
     *
     * @param keyword 关键字
     * @return 用户数量
     */
    long countSearchUsers(String keyword);

    /**
     * 统计用户粉丝数。
     *
     * @param userId 用户ID
     * @return 粉丝数
     */
    int countFollowers(Long userId);

    /**
     * 统计用户已发布文章数。
     *
     * @param userId 用户ID
     * @return 已发布文章数
     */
    int countPublishedArticles(Long userId);

    /**
     * 批量统计多个用户的粉丝数。
     *
     * @param userIds 用户ID列表
     * @return userId -> followerCount 映射
     */
    Map<Long, Integer> countFollowersBatchByIds(List<Long> userIds);

    /**
     * 批量统计多个用户的已发布文章数。
     *
     * @param userIds 用户ID列表
     * @return userId -> articleCount 映射
     */
    Map<Long, Integer> countPublishedArticlesBatchByIds(List<Long> userIds);

    /**
     * 查询推荐作者。
     *
     * @param limit 限制数量
     * @return 推荐作者列表
     */
    List<User> findRecommended(int limit);
}
