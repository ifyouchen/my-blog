package com.myblog.growth.domain.repository;

import com.myblog.growth.domain.model.valueobject.ExpJournal;

import java.util.List;

/**
 * 经验流水 Repository 接口.
 */
public interface ExpJournalRepository {

    /**
     * 保存经验流水（仅 INSERT，不允许更新）.
     *
     * @param journal 经验流水
     * @return 插入行数（0=幂等键冲突，1=成功）
     */
    int insertIgnore(ExpJournal journal);

    /**
     * 查询用户最近 N 条经验流水.
     *
     * @param userId 用户 ID
     * @param limit  条数
     * @return 经验流水列表
     */
    List<ExpJournal> findRecentByUserId(Long userId, int limit);
}

