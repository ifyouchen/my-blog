package com.myblog.domain.model.valueobject;

import java.util.Objects;

/**
 * 专栏 ID。
 *
 * @author Codex
 * @since 1.0.0
 */
public final class ColumnId {

    /** 专栏 ID 的数字值。 */
    private final Long value;

    /**
     * 创建专栏 ID。
     *
     * @param value 专栏 ID 原始值
     */
    public ColumnId(Long value) {
        this.value = value;
    }

    /**
     * 获取专栏 ID 原始值。
     *
     * @return 专栏 ID 原始值
     */
    public Long getValue() {
        return value;
    }

    /**
     * 判断两个专栏 ID 是否相等。
     *
     * @param o 待比较对象
     * @return 若相等则返回 {@code true}
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ColumnId)) {
            return false;
        }
        ColumnId columnId = (ColumnId) o;
        return Objects.equals(value, columnId.value);
    }

    /**
     * 计算专栏 ID 哈希值。
     *
     * @return 哈希值
     */
    @Override
    public int hashCode() {
        return Objects.hash(value);
    }
}
