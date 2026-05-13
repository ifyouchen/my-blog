package com.myblog.domain.model.valueobject;

/**
 * 分类 ID 値对象。
 *
 * @author Codex
 * @since 1.0.0
 */
public class CategoryId {

    /** 分类 ID 的数字值。 */
    private final Long value;

    /**
     * 创建分类 ID。
     *
     * @param value 分类 ID 原始值
     */
    public CategoryId(Long value) {
        this.value = value;
    }

    /**
     * 获取分类 ID 原始值。
     *
     * @return 分类 ID 原始值
     */
    public Long getValue() {
        return value;
    }

    /**
     * 判断两个分类 ID 是否相等。
     *
     * @param obj 待比较对象
     * @return 若相等则返回 {@code true}
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof CategoryId)) {
            return false;
        }
        CategoryId other = (CategoryId) obj;
        return value != null && value.equals(other.value);
    }

    /**
     * 计算分类 ID 哈希值。
     *
     * @return 哈希值
     */
    @Override
    public int hashCode() {
        return value != null ? value.hashCode() : 0;
    }
}