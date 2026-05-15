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
}

