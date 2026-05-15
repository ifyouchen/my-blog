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

    /**
     * 管理端：查询全部规则（含已软删）.
     *
     * @return 规则列表
     */
    List<PointRuleConfigDO> selectAllForAdmin();

    /**
     * 根据主键查询（不排除软删除）.
     *
     * @param id 主键
     * @return 规则 DO，不存在时返回 null
     */
    PointRuleConfigDO selectById(@Param("id") Long id);

    /**
     * 根据来源类型查询（含已软删）.
     *
     * @param sourceType 来源类型
     * @return 规则 DO，不存在时返回 null
     */
    PointRuleConfigDO selectBySourceTypeIncludingDeleted(@Param("sourceType") String sourceType);

    /**
     * 查询已软删的规则（用于新增时恢复）.
     *
     * @param sourceType 来源类型
     * @return 规则 DO，不存在时返回 null
     */
    PointRuleConfigDO selectDeletedBySourceType(@Param("sourceType") String sourceType);

    /**
     * 新增规则.
     *
     * @param do_ 规则 DO（返回时自动回填 id）
     * @return 影响行数
     */
    int insert(PointRuleConfigDO do_);

    /**
     * 乐观锁更新规则.
     *
     * @param do_ 规则 DO（必须含 id 和 version）
     * @return 影响行数（0 表示版本冲突或记录不存在）
     */
    int updateCAS(PointRuleConfigDO do_);

    /**
     * 软删除（乐观锁 CAS）.
     *
     * @param id      主键
     * @param version 版本号
     * @param operator 操作人
     * @param reason   删除原因
     * @return 影响行数（0 表示版本冲突）
     */
    int softDeleteCAS(@Param("id") Long id, @Param("version") int version,
                      @Param("operator") String operator, @Param("reason") String reason);

    /**
     * 恢复软删并更新字段（乐观锁 CAS）.
     *
     * @param do_ 规则 DO（必须含 id 和 version）
     * @return 影响行数（0 表示版本冲突）
     */
    int restoreCAS(PointRuleConfigDO do_);
}
