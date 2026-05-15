package com.myblog.growth.infrastructure.repository.persistence.mapper;

import com.myblog.growth.infrastructure.repository.persistence.entity.LevelThresholdConfigDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 等级阈值配置 MyBatis Mapper.
 * <p>
 * 对应数据库表 {@code level_threshold_config}，
 * XML 定义于 {@code mapper/growth/LevelThresholdConfigMapper.xml}。
 * </p>
 */
@Mapper
public interface LevelThresholdConfigMapper {

    /**
     * 查询所有已启用且未软删除的等级阈值，按 level ASC 排列.
     *
     * @return 等级阈值列表
     */
    List<LevelThresholdConfigDO> selectAllEnabled();

    /**
     * 批量保存等级阈值（INSERT ... ON DUPLICATE KEY UPDATE）.
     * <p>
     * 以 level 为唯一键，已存在则更新 minExp、levelName、description、operator。
     * </p>
     *
     * @param list 等级阈值列表（不能为空）
     */
    void batchUpsert(List<LevelThresholdConfigDO> list);
}

