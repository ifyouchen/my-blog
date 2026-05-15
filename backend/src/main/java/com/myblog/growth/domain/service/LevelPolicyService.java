package com.myblog.growth.domain.service;

import com.myblog.growth.domain.model.valueobject.LevelThreshold;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 等级策略领域服务.
 * <p>
 * 负责根据经验值和等级阈值配置，计算用户当前等级、升级进度、
 * 距下一级所需经验等。不含任何持久化依赖，便于单元测试。
 * </p>
 */
@Service
public class LevelPolicyService {

    /**
     * 根据经验值计算应达到的等级.
     * <p>
     * 从阈值列表尾部向前遍历，找到 {@code exp >= minExp} 的最高等级。
     * 若 {@code thresholds} 为空，返回 1（最低等级）。
     * </p>
     *
     * @param exp        当前累计经验值
     * @param thresholds 等级阈值列表（必须按 level ASC 排列）
     * @return 计算得到的等级值（≥ 1）
     */
    public int computeLevel(int exp, List<LevelThreshold> thresholds) {
        if (thresholds == null || thresholds.isEmpty()) {
            return 1;
        }
        int result = thresholds.get(0).getLevel();
        for (LevelThreshold threshold : thresholds) {
            if (exp >= threshold.getMinExp()) {
                result = threshold.getLevel();
            }
        }
        return result;
    }

    /**
     * 计算距下一级还需要的经验值.
     * <p>
     * 若已是最高等级，返回 0。
     * </p>
     *
     * @param exp          当前累计经验值
     * @param currentLevel 当前等级
     * @param thresholds   等级阈值列表（按 level ASC）
     * @return 距下一级所需经验量，已是最高级返回 0
     */
    public int expToNextLevel(int exp, int currentLevel, List<LevelThreshold> thresholds) {
        for (LevelThreshold threshold : thresholds) {
            if (threshold.getLevel() == currentLevel + 1) {
                return Math.max(0, threshold.getMinExp() - exp);
            }
        }
        return 0;
    }

    /**
     * 计算当前等级的升级进度百分比（0~100）.
     * <p>
     * 计算公式：{@code (exp - currentMin) / (nextMin - currentMin) * 100}。
     * 若已是最高等级，返回 100。
     * </p>
     *
     * @param exp          当前累计经验值
     * @param currentLevel 当前等级
     * @param thresholds   等级阈值列表（按 level ASC）
     * @return 进度百分比（0~100）
     */
    public int progressPercent(int exp, int currentLevel, List<LevelThreshold> thresholds) {
        int currentMin = 0;
        int nextMin = -1;

        for (LevelThreshold threshold : thresholds) {
            if (threshold.getLevel() == currentLevel) {
                currentMin = threshold.getMinExp();
            } else if (threshold.getLevel() == currentLevel + 1) {
                nextMin = threshold.getMinExp();
            }
        }

        // 已是最高等级
        if (nextMin < 0) {
            return 100;
        }

        int range = nextMin - currentMin;
        if (range <= 0) {
            return 100;
        }

        int progress = (int) (((long) (exp - currentMin) * 100) / range);
        return Math.max(0, Math.min(100, progress));
    }

    /**
     * 根据等级获取等级名称.
     *
     * @param level      等级值
     * @param thresholds 等级阈值列表
     * @return 等级名称，未找到时返回空字符串
     */
    public String getLevelName(int level, List<LevelThreshold> thresholds) {
        for (LevelThreshold threshold : thresholds) {
            if (threshold.getLevel() == level) {
                return threshold.getLevelName();
            }
        }
        return "";
    }
}

