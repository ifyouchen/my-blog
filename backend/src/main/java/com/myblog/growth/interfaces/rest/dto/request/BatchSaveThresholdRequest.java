package com.myblog.growth.interfaces.rest.dto.request;

import javax.validation.Valid;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import java.util.List;

/**
 * 批量保存等级阈值请求体.
 * <p>
 * 使用 {@code PUT /api/admin/growth/thresholds} 批量覆盖保存等级阈值。
 * </p>
 */
public class BatchSaveThresholdRequest {

    /**
     * 等级阈值列表，至少包含 1 条.
     */
    @NotEmpty(message = "等级阈值列表不能为空")
    @Valid
    private List<ThresholdItem> thresholds;

    /** 操作人（由网关注入）. */
    @NotBlank(message = "operator 不能为空")
    private String operator;

    /** 获取等级阈值列表. */
    public List<ThresholdItem> getThresholds() { return thresholds; }
    /** 设置等级阈值列表. */
    public void setThresholds(List<ThresholdItem> thresholds) { this.thresholds = thresholds; }
    /** 获取操作人. */
    public String getOperator() { return operator; }
    /** 设置操作人. */
    public void setOperator(String operator) { this.operator = operator; }

    /**
     * 等级阈值条目.
     */
    public static class ThresholdItem {

        /** 等级值（≥ 1）. */
        @Min(value = 1, message = "level 最小值为 1")
        private int level;

        /** 达到此等级的最低经验值（≥ 0）. */
        @Min(value = 0, message = "minExp 不能为负数")
        private int minExp;

        /** 等级名称（不能为空）. */
        @NotBlank(message = "levelName 不能为空")
        private String levelName;

        /** 等级描述. */
        private String description;

        /** 乐观锁版本号（已存在时回传，新增时为 0）. */
        private int version;

        /** 获取等级值. */
        public int getLevel() { return level; }
        /** 设置等级值. */
        public void setLevel(int level) { this.level = level; }
        /** 获取最低经验. */
        public int getMinExp() { return minExp; }
        /** 设置最低经验. */
        public void setMinExp(int minExp) { this.minExp = minExp; }
        /** 获取等级名称. */
        public String getLevelName() { return levelName; }
        /** 设置等级名称. */
        public void setLevelName(String levelName) { this.levelName = levelName; }
        /** 获取描述. */
        public String getDescription() { return description; }
        /** 设置描述. */
        public void setDescription(String description) { this.description = description; }
        /** 获取版本号. */
        public int getVersion() { return version; }
        /** 设置版本号. */
        public void setVersion(int version) { this.version = version; }
    }
}

