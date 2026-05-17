package com.myblog.growth.application.service;

import com.myblog.growth.domain.model.valueobject.ConsecutiveSignInRewardConfig;
import com.myblog.growth.domain.model.valueobject.CumulativeSignInRewardConfig;
import com.myblog.growth.domain.model.valueobject.LevelRewardConfig;
import com.myblog.growth.domain.model.valueobject.RewardGrantLog;
import com.myblog.growth.domain.repository.ConsecutiveSignInRewardRepository;
import com.myblog.growth.domain.repository.CumulativeSignInRewardRepository;
import com.myblog.growth.domain.repository.LevelRewardRepository;
import com.myblog.growth.domain.repository.RewardGrantLogRepository;
import com.myblog.growth.interfaces.rest.dto.response.AdminRewardGrantLogVO;
import com.myblog.growth.shared.exception.GrowthBusinessException;
import com.myblog.growth.shared.exception.GrowthErrorCode;
import com.myblog.shared.result.PageResult;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 管理端奖励配置与奖励记录应用服务.
 *
 * @author Codex
 * @since 2026-05-17
 */
@Service
public class RewardConfigAppService {

    private static final String REWARD_TYPE_LEVEL_UP = "LEVEL_UP";
    private static final String REWARD_TYPE_CONSECUTIVE = "CONSECUTIVE_SIGN";
    private static final String REWARD_TYPE_CUMULATIVE = "CUMULATIVE_SIGN";

    private final LevelRewardRepository levelRewardRepository;
    private final ConsecutiveSignInRewardRepository consecutiveRewardRepository;
    private final CumulativeSignInRewardRepository cumulativeRewardRepository;
    private final RewardGrantLogRepository rewardGrantLogRepository;

    /**
     * 构造注入.
     *
     * @param levelRewardRepository       等级奖励仓储
     * @param consecutiveRewardRepository 连续签到奖励仓储
     * @param cumulativeRewardRepository  累计签到奖励仓储
     * @param rewardGrantLogRepository    奖励领取记录仓储
     */
    public RewardConfigAppService(LevelRewardRepository levelRewardRepository,
                                  ConsecutiveSignInRewardRepository consecutiveRewardRepository,
                                  CumulativeSignInRewardRepository cumulativeRewardRepository,
                                  RewardGrantLogRepository rewardGrantLogRepository) {
        this.levelRewardRepository = levelRewardRepository;
        this.consecutiveRewardRepository = consecutiveRewardRepository;
        this.cumulativeRewardRepository = cumulativeRewardRepository;
        this.rewardGrantLogRepository = rewardGrantLogRepository;
    }

    /**
     * 查询全部等级奖励配置.
     *
     * @return 配置列表
     */
    public List<LevelRewardConfig> listLevelRewards() {
        return levelRewardRepository.findAll();
    }

    /**
     * 查询全部连续签到奖励配置.
     *
     * @return 配置列表
     */
    public List<ConsecutiveSignInRewardConfig> listConsecutiveRewards() {
        return consecutiveRewardRepository.findAll();
    }

    /**
     * 查询全部累计签到里程碑配置.
     *
     * @return 配置列表
     */
    public List<CumulativeSignInRewardConfig> listCumulativeRewards() {
        return cumulativeRewardRepository.findAll();
    }

    /**
     * 新增等级奖励配置.
     *
     * @param request 配置
     * @return 配置ID
     */
    @Transactional(rollbackFor = Exception.class)
    public Long createLevelReward(LevelRewardConfig request) {
        validateLevelReward(request);
        ensureLevelNotDuplicated(request.getLevel(), null);

        LevelRewardConfig deleted = levelRewardRepository.findDeletedByLevel(request.getLevel()).orElse(null);
        if (deleted != null) {
            LevelRewardConfig restored = buildLevelReward(deleted.getId(), request, deleted.getVersion());
            if (!levelRewardRepository.restore(restored)) {
                throw new GrowthBusinessException(GrowthErrorCode.OPTIMISTIC_LOCK_CONFLICT,
                        "等级奖励配置恢复失败，请刷新后重试");
            }
            return deleted.getId();
        }

        LevelRewardConfig next = buildLevelReward(null, request, 0);
        return levelRewardRepository.insert(next);
    }

    /**
     * 更新等级奖励配置.
     *
     * @param request 配置
     */
    @Transactional(rollbackFor = Exception.class)
    public void updateLevelReward(LevelRewardConfig request) {
        if (request == null || request.getId() == null) {
            throw new GrowthBusinessException(GrowthErrorCode.PARAM_INVALID, "等级奖励配置 ID 不能为空");
        }
        validateLevelReward(request);

        LevelRewardConfig current = levelRewardRepository.findById(request.getId())
                .orElseThrow(() -> new GrowthBusinessException(GrowthErrorCode.PARAM_INVALID, "等级奖励配置不存在"));

        ensureLevelNotDuplicated(request.getLevel(), request.getId());

        LevelRewardConfig next = buildLevelReward(current.getId(), request, request.getVersion());

        if (!levelRewardRepository.update(next)) {
            throw new GrowthBusinessException(GrowthErrorCode.OPTIMISTIC_LOCK_CONFLICT,
                    "等级奖励配置更新失败，请刷新后重试");
        }
    }

    /**
     * 删除等级奖励配置.
     *
     * @param id      配置ID
     * @param version 当前版本
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteLevelReward(Long id, int version) {
        if (id == null || id <= 0) {
            throw new GrowthBusinessException(GrowthErrorCode.PARAM_INVALID, "等级奖励配置 ID 不合法");
        }
        if (!levelRewardRepository.softDelete(id, version)) {
            throw new GrowthBusinessException(GrowthErrorCode.OPTIMISTIC_LOCK_CONFLICT,
                    "等级奖励配置删除失败，请刷新后重试");
        }
    }

    /**
     * 新增连续签到奖励配置.
     *
     * @param request 配置
     * @return 配置ID
     */
    @Transactional(rollbackFor = Exception.class)
    public Long createConsecutiveReward(ConsecutiveSignInRewardConfig request) {
        validateConsecutiveReward(request);
        ensureConsecutiveRangeNotOverlap(request, null);
        ConsecutiveSignInRewardConfig next = buildConsecutiveReward(null, request, 0);
        return consecutiveRewardRepository.insert(next);
    }

    /**
     * 更新连续签到奖励配置.
     *
     * @param request 配置
     */
    @Transactional(rollbackFor = Exception.class)
    public void updateConsecutiveReward(ConsecutiveSignInRewardConfig request) {
        if (request == null || request.getId() == null) {
            throw new GrowthBusinessException(GrowthErrorCode.PARAM_INVALID, "连续签到奖励配置 ID 不能为空");
        }
        validateConsecutiveReward(request);

        ConsecutiveSignInRewardConfig current = consecutiveRewardRepository.findById(request.getId())
                .orElseThrow(() -> new GrowthBusinessException(GrowthErrorCode.PARAM_INVALID, "连续签到奖励配置不存在"));

        ensureConsecutiveRangeNotOverlap(request, request.getId());

        ConsecutiveSignInRewardConfig next = buildConsecutiveReward(current.getId(), request, request.getVersion());

        if (!consecutiveRewardRepository.update(next)) {
            throw new GrowthBusinessException(GrowthErrorCode.OPTIMISTIC_LOCK_CONFLICT,
                    "连续签到奖励配置更新失败，请刷新后重试");
        }
    }

    /**
     * 删除连续签到奖励配置.
     *
     * @param id      配置ID
     * @param version 当前版本
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteConsecutiveReward(Long id, int version) {
        if (id == null || id <= 0) {
            throw new GrowthBusinessException(GrowthErrorCode.PARAM_INVALID, "连续签到奖励配置 ID 不合法");
        }
        if (!consecutiveRewardRepository.softDelete(id, version)) {
            throw new GrowthBusinessException(GrowthErrorCode.OPTIMISTIC_LOCK_CONFLICT,
                    "连续签到奖励配置删除失败，请刷新后重试");
        }
    }

    /**
     * 新增累计签到里程碑奖励配置.
     *
     * @param request 配置
     * @return 配置ID
     */
    @Transactional(rollbackFor = Exception.class)
    public Long createCumulativeReward(CumulativeSignInRewardConfig request) {
        validateCumulativeReward(request);
        ensureMilestoneNotDuplicated(request.getMilestoneDays(), null);

        CumulativeSignInRewardConfig deleted =
                cumulativeRewardRepository.findDeletedByMilestoneDays(request.getMilestoneDays()).orElse(null);
        if (deleted != null) {
            CumulativeSignInRewardConfig restored = buildCumulativeReward(deleted.getId(), request, deleted.getVersion());
            if (!cumulativeRewardRepository.restore(restored)) {
                throw new GrowthBusinessException(GrowthErrorCode.OPTIMISTIC_LOCK_CONFLICT,
                        "累计签到里程碑配置恢复失败，请刷新后重试");
            }
            return deleted.getId();
        }

        CumulativeSignInRewardConfig next = buildCumulativeReward(null, request, 0);
        return cumulativeRewardRepository.insert(next);
    }

    /**
     * 更新累计签到里程碑奖励配置.
     *
     * @param request 配置
     */
    @Transactional(rollbackFor = Exception.class)
    public void updateCumulativeReward(CumulativeSignInRewardConfig request) {
        if (request == null || request.getId() == null) {
            throw new GrowthBusinessException(GrowthErrorCode.PARAM_INVALID, "累计签到里程碑配置 ID 不能为空");
        }
        validateCumulativeReward(request);

        CumulativeSignInRewardConfig current = cumulativeRewardRepository.findById(request.getId())
                .orElseThrow(() -> new GrowthBusinessException(GrowthErrorCode.PARAM_INVALID, "累计签到里程碑配置不存在"));

        ensureMilestoneNotDuplicated(request.getMilestoneDays(), request.getId());

        CumulativeSignInRewardConfig next = buildCumulativeReward(current.getId(), request, request.getVersion());

        if (!cumulativeRewardRepository.update(next)) {
            throw new GrowthBusinessException(GrowthErrorCode.OPTIMISTIC_LOCK_CONFLICT,
                    "累计签到里程碑配置更新失败，请刷新后重试");
        }
    }

    /**
     * 删除累计签到里程碑奖励配置.
     *
     * @param id      配置ID
     * @param version 当前版本
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteCumulativeReward(Long id, int version) {
        if (id == null || id <= 0) {
            throw new GrowthBusinessException(GrowthErrorCode.PARAM_INVALID, "累计签到里程碑配置 ID 不合法");
        }
        if (!cumulativeRewardRepository.softDelete(id, version)) {
            throw new GrowthBusinessException(GrowthErrorCode.OPTIMISTIC_LOCK_CONFLICT,
                    "累计签到里程碑配置删除失败，请刷新后重试");
        }
    }

    /**
     * 分页查询奖励领取记录.
     *
     * @param userId     用户ID（可选）
     * @param rewardType 奖励类型（可选）
     * @param page       页码
     * @param size       每页数量
     * @return 分页结果
     */
    public PageResult<AdminRewardGrantLogVO> pageGrantLogs(Long userId, String rewardType, int page, int size) {
        int safePage = Math.max(1, page);
        int safeSize = Math.min(Math.max(1, size), 50);
        String normalizedRewardType = normalizeRewardType(rewardType);
        int offset = (safePage - 1) * safeSize;
        long total = rewardGrantLogRepository.countForAdmin(userId, normalizedRewardType);
        List<RewardGrantLog> logs = rewardGrantLogRepository.findPageForAdmin(userId, normalizedRewardType, offset, safeSize);

        Map<Long, LevelRewardConfig> levelRewardMap = levelRewardRepository.findAll().stream()
                .collect(Collectors.toMap(LevelRewardConfig::getId, item -> item, (left, right) -> left, HashMap::new));
        Map<Long, ConsecutiveSignInRewardConfig> consecutiveMap = consecutiveRewardRepository.findAll().stream()
                .collect(Collectors.toMap(ConsecutiveSignInRewardConfig::getId, item -> item, (left, right) -> left, HashMap::new));
        Map<Long, CumulativeSignInRewardConfig> cumulativeMap = cumulativeRewardRepository.findAll().stream()
                .collect(Collectors.toMap(CumulativeSignInRewardConfig::getId, item -> item, (left, right) -> left, HashMap::new));

        List<AdminRewardGrantLogVO> items = logs.stream()
                .map(log -> toGrantLogVO(log, levelRewardMap, consecutiveMap, cumulativeMap))
                .collect(Collectors.toList());
        return new PageResult<>(items, safePage, safeSize, total);
    }

    private AdminRewardGrantLogVO toGrantLogVO(RewardGrantLog log,
                                               Map<Long, LevelRewardConfig> levelRewardMap,
                                               Map<Long, ConsecutiveSignInRewardConfig> consecutiveMap,
                                               Map<Long, CumulativeSignInRewardConfig> cumulativeMap) {
        AdminRewardGrantLogVO vo = new AdminRewardGrantLogVO();
        vo.setId(log.getId());
        vo.setUserId(log.getUserId());
        vo.setRewardType(log.getRewardType());
        vo.setRewardTypeLabel(rewardTypeLabel(log.getRewardType()));
        vo.setRewardId(log.getRewardId());
        vo.setRewardName(resolveRewardName(log, levelRewardMap, consecutiveMap, cumulativeMap));
        vo.setPointsGranted(log.getPointsGranted());
        vo.setGrantedAt(log.getGrantedAt());
        vo.setRemark(log.getRemark());
        return vo;
    }

    private String resolveRewardName(RewardGrantLog log,
                                     Map<Long, LevelRewardConfig> levelRewardMap,
                                     Map<Long, ConsecutiveSignInRewardConfig> consecutiveMap,
                                     Map<Long, CumulativeSignInRewardConfig> cumulativeMap) {
        if (REWARD_TYPE_LEVEL_UP.equals(log.getRewardType())) {
            LevelRewardConfig config = levelRewardMap.get(log.getRewardId());
            if (config == null) {
                return "等级奖励 #" + log.getRewardId();
            }
            return "Lv." + config.getLevel() + " · " + defaultText(config.getRewardTitle(), "等级奖励");
        }
        if (REWARD_TYPE_CUMULATIVE.equals(log.getRewardType())) {
            CumulativeSignInRewardConfig config = cumulativeMap.get(log.getRewardId());
            if (config == null) {
                return "累计签到里程碑 #" + log.getRewardId();
            }
            return config.getMilestoneDays() + " 天 · " + defaultText(config.getRewardTitle(), "累计签到奖励");
        }
        if (REWARD_TYPE_CONSECUTIVE.equals(log.getRewardType())) {
            ConsecutiveSignInRewardConfig config = consecutiveMap.get(log.getRewardId());
            if (config == null) {
                return "连续签到奖励 #" + log.getRewardId();
            }
            return config.getMinDays() + "~" + displayRangeEnd(config.getMaxDays()) + " 天 · "
                    + defaultText(config.getRewardTier(), "连续签到奖励");
        }
        return "未知奖励 #" + log.getRewardId();
    }

    private String rewardTypeLabel(String rewardType) {
        if (REWARD_TYPE_LEVEL_UP.equals(rewardType)) {
            return "等级奖励";
        }
        if (REWARD_TYPE_CUMULATIVE.equals(rewardType)) {
            return "累计签到里程碑";
        }
        if (REWARD_TYPE_CONSECUTIVE.equals(rewardType)) {
            return "连续签到奖励";
        }
        return rewardType;
    }

    private String normalizeRewardType(String rewardType) {
        String normalized = trimToNull(rewardType);
        if (normalized == null) {
            return null;
        }
        if (REWARD_TYPE_LEVEL_UP.equals(normalized)
                || REWARD_TYPE_CONSECUTIVE.equals(normalized)
                || REWARD_TYPE_CUMULATIVE.equals(normalized)) {
            return normalized;
        }
        throw new GrowthBusinessException(GrowthErrorCode.PARAM_INVALID, "不支持的奖励类型：" + rewardType);
    }

    private boolean isRangeOverlap(int minA, Integer maxA, int minB, Integer maxB) {
        int normalizedMaxA = maxA == null ? Integer.MAX_VALUE : maxA;
        int normalizedMaxB = maxB == null ? Integer.MAX_VALUE : maxB;
        return minA <= normalizedMaxB && minB <= normalizedMaxA;
    }

    private void validateLevelReward(LevelRewardConfig request) {
        if (request == null) {
            throw new GrowthBusinessException(GrowthErrorCode.PARAM_INVALID, "等级奖励配置不能为空");
        }
        if (request.getLevel() <= 0) {
            throw new GrowthBusinessException(GrowthErrorCode.PARAM_INVALID, "等级必须大于 0");
        }
        if (request.getRewardPoints() < 0) {
            throw new GrowthBusinessException(GrowthErrorCode.PARAM_INVALID, "奖励积分不能小于 0");
        }
    }

    private void validateConsecutiveReward(ConsecutiveSignInRewardConfig request) {
        if (request == null) {
            throw new GrowthBusinessException(GrowthErrorCode.PARAM_INVALID, "连续签到奖励配置不能为空");
        }
        if (request.getMinDays() <= 0) {
            throw new GrowthBusinessException(GrowthErrorCode.PARAM_INVALID, "最小连续天数必须大于 0");
        }
        if (request.getMaxDays() != null && request.getMaxDays() < request.getMinDays()) {
            throw new GrowthBusinessException(GrowthErrorCode.PARAM_INVALID, "最大连续天数不能小于最小连续天数");
        }
        if (request.getBonusPoints() < 0) {
            throw new GrowthBusinessException(GrowthErrorCode.PARAM_INVALID, "奖励积分不能小于 0");
        }
    }

    private void validateCumulativeReward(CumulativeSignInRewardConfig request) {
        if (request == null) {
            throw new GrowthBusinessException(GrowthErrorCode.PARAM_INVALID, "累计签到里程碑配置不能为空");
        }
        if (request.getMilestoneDays() <= 0) {
            throw new GrowthBusinessException(GrowthErrorCode.PARAM_INVALID, "里程碑天数必须大于 0");
        }
        if (request.getRewardPoints() < 0) {
            throw new GrowthBusinessException(GrowthErrorCode.PARAM_INVALID, "奖励积分不能小于 0");
        }
    }

    private void ensureLevelNotDuplicated(int level, Long exceptId) {
        boolean duplicated = levelRewardRepository.findAll().stream()
                .anyMatch(item -> !item.getId().equals(exceptId) && item.getLevel() == level);
        if (duplicated) {
            throw new GrowthBusinessException(GrowthErrorCode.RULE_DUPLICATE,
                    "等级 " + level + " 的奖励配置已存在");
        }
    }

    private void ensureConsecutiveRangeNotOverlap(ConsecutiveSignInRewardConfig request, Long exceptId) {
        for (ConsecutiveSignInRewardConfig item : consecutiveRewardRepository.findAll()) {
            if (item.getId().equals(exceptId)) {
                continue;
            }
            if (isRangeOverlap(request.getMinDays(), request.getMaxDays(), item.getMinDays(), item.getMaxDays())) {
                throw new GrowthBusinessException(GrowthErrorCode.RULE_DUPLICATE,
                        "连续签到区间与现有配置冲突：" + item.getMinDays() + "~" + displayRangeEnd(item.getMaxDays()));
            }
        }
    }

    private void ensureMilestoneNotDuplicated(int milestoneDays, Long exceptId) {
        boolean duplicated = cumulativeRewardRepository.findAll().stream()
                .anyMatch(item -> !item.getId().equals(exceptId) && item.getMilestoneDays() == milestoneDays);
        if (duplicated) {
            throw new GrowthBusinessException(GrowthErrorCode.RULE_DUPLICATE,
                    "累计签到 " + milestoneDays + " 天的里程碑配置已存在");
        }
    }

    private LevelRewardConfig buildLevelReward(Long id, LevelRewardConfig request, int version) {
        LevelRewardConfig next = new LevelRewardConfig();
        next.setId(id);
        next.setLevel(request.getLevel());
        next.setRewardPoints(request.getRewardPoints());
        next.setRewardTitle(trimToNull(request.getRewardTitle()));
        next.setDescription(trimToNull(request.getDescription()));
        next.setEnabled(request.isEnabled());
        next.setVersion(version);
        return next;
    }

    private ConsecutiveSignInRewardConfig buildConsecutiveReward(Long id,
                                                                 ConsecutiveSignInRewardConfig request,
                                                                 int version) {
        ConsecutiveSignInRewardConfig next = new ConsecutiveSignInRewardConfig();
        next.setId(id);
        next.setMinDays(request.getMinDays());
        next.setMaxDays(request.getMaxDays());
        next.setBonusPoints(request.getBonusPoints());
        next.setRewardTier(trimToNull(request.getRewardTier()));
        next.setRewardDesc(trimToNull(request.getRewardDesc()));
        next.setEnabled(request.isEnabled());
        next.setVersion(version);
        return next;
    }

    private CumulativeSignInRewardConfig buildCumulativeReward(Long id,
                                                               CumulativeSignInRewardConfig request,
                                                               int version) {
        CumulativeSignInRewardConfig next = new CumulativeSignInRewardConfig();
        next.setId(id);
        next.setMilestoneDays(request.getMilestoneDays());
        next.setRewardPoints(request.getRewardPoints());
        next.setRewardTitle(trimToNull(request.getRewardTitle()));
        next.setBadgeCode(trimToNull(request.getBadgeCode()));
        next.setDescription(trimToNull(request.getDescription()));
        next.setEnabled(request.isEnabled());
        next.setVersion(version);
        return next;
    }

    private String displayRangeEnd(Integer maxDays) {
        return maxDays == null ? "∞" : String.valueOf(maxDays);
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String defaultText(String value, String fallback) {
        return value == null || value.trim().isEmpty() ? fallback : value.trim();
    }
}
