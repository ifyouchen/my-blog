package com.myblog.growth.infrastructure.repository.persistence.mapper;

import com.myblog.growth.infrastructure.repository.persistence.entity.PointRuleConfigDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 积分规则配置 MyBatis Mapper.
 * <p>对应表 {@code point_rule_config}，XML 在 {@code mapper/growth/PointRuleConfigMapper.xml}。</p>
 */
@Mapper
public interface PointRuleConfigMapper {

    /**
     * 根据来源类型查询规则（排除软删除）.
     *
     * @param sourceType 来源类型
     * @return 规则 DO，不存在时返回 null
     */
    PointRuleConfigDO selectBySourceType(@Param("sourceType") String sourceType);

    /**
     * 查询所有已启用且未软删除的规则.
     *
     * @return 规则列表
     */
    List<PointRuleConfigDO> selectAllEnabled();
}

