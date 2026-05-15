package com.myblog.growth.domain.repository;

import com.myblog.growth.domain.model.valueobject.PointJournal;

import java.util.List;

/**
 * 积分流水 Repository 接口.
 */
public interface PointJournalRepository {

    /**
     * 插入积分流水（INSERT IGNORE，幂等键冲突时静默忽略）.
     *
     * @param journal 积分流水值对象
     * @return 插入行数（1=成功，0=幂等键重复）
     */
    int insertIgnore(PointJournal journal);

    /**
     * 查询用户近期积分流水（按创建时间倒序）.
     *
     * @param userId 用户 ID
     * @param limit  最多返回条数
     * @return 积分流水列表
     */
    List<PointJournal> findRecentByUserId(Long userId, int limit);

    /**
     * 按来源类型分页查询积分流水.
     *
     * @param userId     用户 ID
     * @param sourceType 来源类型（null 表示不筛选）
     * @param page       页码（从 1 开始）
     * @param size       每页条数
     * @return 积分流水列表
     */
    List<PointJournal> findPageByUserId(Long userId, String sourceType, int page, int size);

    /**
     * 统计用户积分流水总数.
     *
     * @param userId     用户 ID
     * @param sourceType 来源类型（null 表示不筛选）
     * @return 总数
     */
    long countByUserId(Long userId, String sourceType);
}

