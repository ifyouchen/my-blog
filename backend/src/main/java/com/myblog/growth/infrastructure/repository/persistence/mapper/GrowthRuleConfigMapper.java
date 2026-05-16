package com.myblog.growth.infrastructure.repository.persistence.mapper;

import com.myblog.growth.infrastructure.repository.persistence.entity.GrowthRuleConfigDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 经验规则配置 MyBatis Mapper.
 * <p>
 * 对应数据库表 {@code growth_rule_config}，
 * XML 定义于 {@code mapper/growth/GrowthRuleConfigMapper.xml}。
 * </p>
 */
@Mapper
public interface GrowthRuleConfigMapper {

    /**
     * 根据事件类型和角色查询有效规则（deleted_at IS NULL）.
     *
     * @param eventType 行为类型
     * @param role      角色（ACTOR / AUTHOR）
     * @return 规则 DO，不存在时返回 null
     */
    GrowthRuleConfigDO selectByEventTypeAndRole(@Param("eventType") String eventType,
                                                @Param("role") String role);

    /**
     * 查询所有已启用且未软删除的规则.
     *
     * @return 规则列表
     */
    List<GrowthRuleConfigDO> selectAllEnabled();

    /**
     * 插入新规则，回填自增主键.
     *
     * @param rule 规则 DO
     */
    void insert(GrowthRuleConfigDO rule);

    /**
     * 更新规则（乐观锁 CAS，版本号自增）.
     *
     * @param rule 包含最新字段和当前 version 的规则 DO
     * @return 更新行数（1=成功，0=版本冲突）
     */
    int updateCAS(GrowthRuleConfigDO rule);

    /**
     * 软删除规则（乐观锁 CAS）.
     *
     * @param id       规则 ID
     * @param version  当前版本号
     * @param operator 操作人
     * @param reason   删除原因
     * @return 更新行数（1=成功，0=版本冲突或不存在）
     */
    int softDeleteCAS(@Param("id") Long id,
                      @Param("version") int version,
                      @Param("operator") String operator,
                      @Param("reason") String reason);
}

