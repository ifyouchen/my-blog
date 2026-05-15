package com.myblog.growth.domain.repository;

import com.myblog.growth.domain.model.valueobject.LevelThreshold;

import java.util.List;

/**
 * 等级阈值配置 Repository 接口.
 */
public interface LevelThresholdRepository {

    /**
     * 查询所有启用的等级阈值，按 level 升序排列.
     * <p>
     * 等级计算服务依赖此列表的有序性，调用方可缓存结果（建议 TTL = 5min）。
     * </p>
     *
     * @return 已启用的等级阈值列表，按 level ASC
     */
    List<LevelThreshold> findAllEnabled();

    /**
     * 批量保存等级阈值（INSERT ... ON DUPLICATE KEY UPDATE）.
     * <p>
     * 以 level 为唯一键，存在则更新，不存在则新增。整批操作在同一事务内。
     * </p>
     *
     * @param thresholds 等级阈值列表
     * @return 实际保存条数
     */
    int batchSave(List<LevelThreshold> thresholds);
}

