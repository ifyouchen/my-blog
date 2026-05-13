package com.myblog.infrastructure.repository.persistence.mapper;

import com.myblog.infrastructure.repository.persistence.entity.UserSearchHistoryDO;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 用户搜索历史 MyBatis Mapper。
 *
 * @author Codex
 * @since 1.0.0
 */
public interface UserSearchHistoryMapper {

    /**
     * 根据 ID 查询搜索历史记录。
     *
     * @param id 搜索历史记录 ID
     * @return 搜索历史数据对象
     */
    UserSearchHistoryDO selectById(@Param("id") Long id);

    /**
     * 分页查询用户的搜索历史列表。
     *
     * @param userId 用户 ID
     * @param offset 偏移量
     * @param limit  限制数量
     * @return 搜索历史列表
     */
    List<UserSearchHistoryDO> selectByUserId(@Param("userId") Long userId,
                                             @Param("offset") int offset,
                                             @Param("limit") int limit);

    /**
     * 统计用户的搜索历史数量。
     *
     * @param userId 用户 ID
     * @return 搜索历史数量
     */
    long countByUserId(@Param("userId") Long userId);

    /**
     * 根据用户 ID 和关键字查询搜索历史记录。
     *
     * @param userId  用户 ID
     * @param keyword 搜索关键字
     * @return 搜索历史数据对象
     */
    UserSearchHistoryDO selectByUserIdAndKeyword(@Param("userId") Long userId,
                                                 @Param("keyword") String keyword);

    /**
     * 插入搜索历史记录。
     *
     * @param userSearchHistoryDO 搜索历史数据对象
     * @return 影响行数
     */
    int insert(UserSearchHistoryDO userSearchHistoryDO);

    /**
     * 更新搜索历史记录。
     *
     * @param userSearchHistoryDO 搜索历史数据对象
     * @return 影响行数
     */
    int update(UserSearchHistoryDO userSearchHistoryDO);

    /**
     * 删除用户的所有搜索历史记录。
     *
     * @param userId 用户 ID
     * @return 影响行数
     */
    int deleteByUserId(@Param("userId") Long userId);

    /**
     * 查询热门搜索关键字列表。
     *
     * @param limit 返回数量限制
     * @return 热门关键字列表
     */
    List<String> selectHotKeywords(@Param("limit") int limit);

    /**
     * 根据主键统计搜索历史记录数量。
     *
     * @param id 搜索历史记录 ID
     * @return 记录数量
     */
    int countById(@Param("id") Long id);

    /**
     * 查询下一个搜索历史记录 ID。
     *
     * @return 下一个搜索历史记录 ID
     */
    Long selectNextId();
}