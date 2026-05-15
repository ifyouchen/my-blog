package com.myblog.growth.domain.repository;

import com.myblog.growth.domain.model.valueobject.PointRule;

import java.util.List;
import java.util.Optional;

/**
 * 积分规则 Repository 接口.
 */
public interface PointRuleRepository {

    /**
     * 根据来源类型查询积分规则.
     *
     * @param sourceType 来源类型（对应 PointEventType 枚举值）
     * @return 积分规则（Optional），不存在时为 empty
     */
    Optional<PointRule> findBySourceType(String sourceType);

    /**
     * 查询所有已启用且生效的积分规则.
     *
     * @return 规则列表
     */
    List<PointRule> findAllEnabled();

    /**
     * 管理端：查询全部规则（含已软删）.
     *
     * @return 规则列表
     */
    List<PointRule> findAllForAdmin();

    /**
     * 根据主键查询（不排除软删除）.
     *
     * @param id 主键
     * @return 积分规则（Optional），不存在时为 empty
     */
    Optional<PointRule> findById(Long id);

    /**
     * 根据来源类型查询（含已软删）.
     *
     * @param sourceType 来源类型
     * @return 积分规则（Optional），不存在时为 empty
     */
    Optional<PointRule> findBySourceTypeIncludingDeleted(String sourceType);

    /**
     * 查询已软删的规则（用于新增时恢复场景）.
     *
     * @param sourceType 来源类型
     * @return 积分规则（Optional），已软删的记录；不存在时为 empty
     */
    Optional<PointRule> findDeletedBySourceType(String sourceType);

    /**
     * 新增规则.
     *
     * @param rule 积分规则
     * @return 自增主键 ID
     */
    Long save(PointRule rule);

    /**
     * 乐观锁更新规则.
     *
     * @param rule 积分规则（必须含 id 和 version）
     * @return true 表示更新成功，false 表示版本冲突
     */
    boolean update(PointRule rule);

    /**
     * 软删除（乐观锁 CAS）.
     *
     * @param id       主键
     * @param version  版本号
     * @param operator 操作人
     * @param reason   删除原因
     * @return true 表示删除成功，false 表示版本冲突
     */
    boolean softDelete(Long id, int version, String operator, String reason);

    /**
     * 恢复软删记录并更新字段（CAS）.
     * <p>将 deleted_at 置 NULL，同时更新其他字段，version +1。</p>
     *
     * @param rule 积分规则（必须含 id 和 version）
     * @return true 表示恢复成功，false 表示版本冲突
     */
    boolean restore(PointRule rule);
}
