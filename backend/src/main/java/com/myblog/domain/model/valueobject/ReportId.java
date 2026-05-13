package com.myblog.domain.model.valueobject;

import java.io.Serializable;
import java.util.Objects;

/**
 * 举报 ID 值对象。
 *
 * @author Codex
 * @since 1.0.0
 */
public class ReportId implements Serializable {

    /** 举报 ID 的数字值，必须大于 0。 */
    private final Long value;

    /**
     * 创建举报 ID。
     *
     * @param value 举报 ID 原始值
     * @throws IllegalArgumentException 若 value 为 null 或小于等于 0
     */
    public ReportId(Long value) {
        if (value == null || value.longValue() <= 0L) {
            throw new IllegalArgumentException("举报ID不能为空");
        }
        this.value = value;
    }

    /**
     * 获取举报 ID 原始值。
     *
     * @return 举报 ID 原始值
     */
    public Long getValue() {
        return value;
    }

    /**
     * 判断两个举报 ID 是否相等。
     *
     * @param o 待比较对象
     * @return 若相等则返回 {@code true}
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ReportId)) {
            return false;
        }
        ReportId reportId = (ReportId) o;
        return Objects.equals(value, reportId.value);
    }

    /**
     * 计算举报 ID 哈希值。
     *
     * @return 哈希值
     */
    @Override
    public int hashCode() {
        return Objects.hash(value);
    }
}
