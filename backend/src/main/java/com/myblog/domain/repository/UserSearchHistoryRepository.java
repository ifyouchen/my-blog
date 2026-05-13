package com.myblog.domain.repository;

import com.myblog.infrastructure.repository.persistence.entity.UserSearchHistoryDO;

import java.util.List;

/**
 * 用户搜索历史仓储接口。
 *
 * @author my-blog
 * @since 1.0.0
 */
public interface UserSearchHistoryRepository {

    /**
     * 根据搜索记录 ID 查询搜索历史。
     *
     * @param id 搜索记录 ID
     * @return 搜索历史数据对象，不存在时返回 null
     */
    UserSearchHistoryDO findById(Long id);

    /**
     * 分页查询用户的搜索历史。
     *
     * @param userId   用户 ID
     * @param page     页码
     * @param pageSize 每页大小
     * @return 搜索历史列表
     */
    List<UserSearchHistoryDO> findByUserId(Long userId, int page, int pageSize);

    /**
     * 统计用户的搜索历史数量。
     *
     * @param userId 用户 ID
     * @return 搜索历史数量
     */
    long countByUserId(Long userId);

    /**
     * 根据用户 ID 和关键字查询搜索历史。
     *
     * @param userId  用户 ID
     * @param keyword 搜索关键字
     * @return 搜索历史数据对象，不存在时返回 null
     */
    UserSearchHistoryDO findByUserIdAndKeyword(Long userId, String keyword);

    /**
     * 保存搜索历史（新增）。
     *
     * @param userSearchHistoryDO 搜索历史数据对象
     */
    void save(UserSearchHistoryDO userSearchHistoryDO);

    /**
     * 更新搜索历史。
     *
     * @param userSearchHistoryDO 搜索历史数据对象
     */
    void update(UserSearchHistoryDO userSearchHistoryDO);

    /**
     * 删除用户的所有搜索历史。
     *
     * @param userId 用户 ID
     */
    void deleteByUserId(Long userId);

    /**
     * 查询热门搜索关键字。
     *
     * @param limit 返回数量限制
     * @return 热门关键字列表
     */
    List<String> findHotKeywords(int limit);

    /**
     * 生成下一个搜索历史记录 ID。
     *
     * @return 搜索历史记录 ID
     */
    Long nextId();
}