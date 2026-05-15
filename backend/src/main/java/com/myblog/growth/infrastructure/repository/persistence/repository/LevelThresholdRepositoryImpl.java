package com.myblog.growth.infrastructure.repository.persistence.repository;

import com.myblog.growth.domain.model.valueobject.LevelThreshold;
import com.myblog.growth.domain.repository.LevelThresholdRepository;
import com.myblog.growth.infrastructure.repository.persistence.converter.GrowthConverter;
import com.myblog.growth.infrastructure.repository.persistence.entity.LevelThresholdConfigDO;
import com.myblog.growth.infrastructure.repository.persistence.mapper.LevelThresholdConfigMapper;
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

    /**
     * 构造注入 Mapper.
     *
     * @param mapper 等级阈值配置 Mapper
     */
    public LevelThresholdRepositoryImpl(LevelThresholdConfigMapper mapper) {
        this.mapper = mapper;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<LevelThreshold> findAllEnabled() {
        return mapper.selectAllEnabled().stream()
                .map(GrowthConverter::toDomain)
                .collect(Collectors.toList());
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
        return doList.size();
    }
}

