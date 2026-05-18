package com.myblog.growth.infrastructure.repository.persistence.repository;

import com.github.benmanes.caffeine.cache.Cache;
import com.myblog.growth.domain.model.valueobject.LevelThreshold;
import com.myblog.growth.domain.repository.LevelThresholdRepository;
import com.myblog.growth.infrastructure.repository.persistence.converter.GrowthConverter;
import com.myblog.growth.infrastructure.repository.persistence.entity.LevelThresholdConfigDO;
import com.myblog.growth.infrastructure.repository.persistence.mapper.LevelThresholdConfigMapper;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 等级阈值 Repository 实现.
 */
@Repository
public class LevelThresholdRepositoryImpl implements LevelThresholdRepository {

    /** 等级阈值配置 MyBatis Mapper. */
    private final LevelThresholdConfigMapper mapper;
    private final Cache<String, List<LevelThreshold>> levelThresholdsCache;

    /**
     * 构造注入 Mapper.
     *
     * @param mapper 等级阈值配置 Mapper
     */
    public LevelThresholdRepositoryImpl(LevelThresholdConfigMapper mapper,
                                        @Qualifier("levelThresholdsCache")
                                        Cache<String, List<LevelThreshold>> levelThresholdsCache) {
        this.mapper = mapper;
        this.levelThresholdsCache = levelThresholdsCache;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<LevelThreshold> findAllEnabled() {
        return levelThresholdsCache.get("all-enabled", key -> mapper.selectAllEnabled().stream()
                .map(GrowthConverter::toDomain)
                .collect(Collectors.toList()));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int batchSave(List<LevelThreshold> thresholds) {
        if (thresholds == null || thresholds.isEmpty()) {
            return 0;
        }
        List<LevelThresholdConfigDO> doList = thresholds.stream()
                .map(GrowthConverter::toDO)
                .collect(Collectors.toList());
        mapper.batchUpsert(doList);
        levelThresholdsCache.invalidateAll();
        return doList.size();
    }
}

