package com.myblog.growth.application.service;

import com.myblog.growth.domain.model.valueobject.PointRule;
import com.myblog.growth.domain.repository.PointRuleRepository;
import com.myblog.growth.shared.exception.GrowthBusinessException;
import com.myblog.growth.shared.exception.GrowthErrorCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * 积分规则管理应用服务.
 * <p>
 * 供管理端接口调用，负责积分规则的增删改查。
 * 新增时处理同 source_type 软删记录的「恢复并更新」，避免唯一键冲突。
 * 更新和删除均使用乐观锁 CAS。
 * </p>
 */
@Service
public class PointRuleAppService {

    private static final Logger log = LoggerFactory.getLogger(PointRuleAppService.class);

    private final PointRuleRepository pointRuleRepository;

    public PointRuleAppService(PointRuleRepository pointRuleRepository) {
        this.pointRuleRepository = pointRuleRepository;
    }

    /**
     * 管理端查询全部积分规则（含已软删）.
     */
    public List<PointRule> listAllRules() {
        return pointRuleRepository.findAllForAdmin();
    }

    /**
     * 新增积分规则.
     * <p>
     * 如果同 source_type 已存在软删记录，则恢复并更新。
     * </p>
     *
     * @param rule 新规则（id 应为 null）
     * @return 新规则 ID
     */
    @Transactional(rollbackFor = Exception.class)
    public Long createRule(PointRule rule) {
        // 检查同 source_type 是否存在软删记录
        Optional<PointRule> deleted = pointRuleRepository.findDeletedBySourceType(rule.getSourceType());
        if (deleted.isPresent()) {
            PointRule old = deleted.get();
            PointRule restored = PointRule.of(
                    old.getId(),
                    rule.getSourceType(),
                    rule.getPointAmount(),
                    rule.getDailyLimit(),
                    null,
                    rule.isEnabled(),
                    null,
                    rule.getOperator(),
                    rule.getReason(),
                    old.getVersion(),
                    null
            );
            boolean updated = pointRuleRepository.restore(restored);
            if (!updated) {
                throw new GrowthBusinessException(GrowthErrorCode.OPTIMISTIC_LOCK_CONFLICT,
                        "恢复积分规则失败，数据已被修改，请刷新后重试");
            }
            log.info("[积分规则] 恢复并更新成功。id={}, sourceType={}, pointAmount={}",
                    old.getId(), rule.getSourceType(), rule.getPointAmount());
            return old.getId();
        }

        // 检查是否已存在活跃记录
        Optional<PointRule> active = pointRuleRepository.findBySourceType(rule.getSourceType());
        if (active.isPresent()) {
            throw new GrowthBusinessException(GrowthErrorCode.RULE_DUPLICATE,
                    "积分规则已存在：sourceType=" + rule.getSourceType());
        }

        Long id = pointRuleRepository.save(rule);
        log.info("[积分规则] 新增成功。id={}, sourceType={}, pointAmount={}",
                id, rule.getSourceType(), rule.getPointAmount());
        return id;
    }

    /**
     * 更新积分规则（乐观锁 CAS）.
     *
     * @param rule 包含最新字段和当前版本号的规则对象
     */
    @Transactional(rollbackFor = Exception.class)
    public void updateRule(PointRule rule) {
        boolean updated = pointRuleRepository.update(rule);
        if (!updated) {
            throw new GrowthBusinessException(GrowthErrorCode.OPTIMISTIC_LOCK_CONFLICT,
                    "积分规则更新失败，可能已被其他人修改，请刷新后重试。id=" + rule.getId());
        }
        log.info("[积分规则] 更新成功。id={}, sourceType={}, pointAmount={}",
                rule.getId(), rule.getSourceType(), rule.getPointAmount());
    }

    /**
     * 软删除积分规则（乐观锁 CAS）.
     *
     * @param id       规则 ID
     * @param version  当前版本号
     * @param operator 操作人
     * @param reason   删除原因
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteRule(Long id, int version, String operator, String reason) {
        boolean deleted = pointRuleRepository.softDelete(id, version, operator, reason);
        if (!deleted) {
            throw new GrowthBusinessException(GrowthErrorCode.OPTIMISTIC_LOCK_CONFLICT,
                    "积分规则删除失败，可能已被其他人修改，请刷新后重试。id=" + id);
        }
        log.info("[积分规则] 软删除成功。id={}, operator={}", id, operator);
    }
}
