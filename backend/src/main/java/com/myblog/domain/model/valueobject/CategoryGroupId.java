package com.myblog.domain.model.valueobject;

/**
 * 分类组 ID 值对象。
 *
 * @author Codex
 * @since 1.0.0
 */
public class CategoryGroupId {

    /** 分类组 ID 的数字值。 */
    private final Long value;

    /**
     * 创建分类组 ID。
     *
     * @param value 分类组 ID 原始值
     */
    public CategoryGroupId(Long value) {
        this.value = value;
    }

    /**
     * 获取分类组 ID 原始值。
     *
     * @return 分类组 ID 原始值
     */
    public Long getValue() {
        return value;
    }

    /**
     * 判断两个分类组 ID 是否相等。
     *
     * @param obj 待比较对象
     * @return 若相等则返回 true
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof CategoryGroupId)) {
            return false;
        }
        CategoryGroupId other = (CategoryGroupId) obj;
        return value != null && value.equals(other.value);
    }

    /**
     * 计算分类组 ID 哈希值。
     *
     * @return 哈希值
     */
    @Override
    public int hashCode() {
        return value != null ? value.hashCode() : 0;
    }
}
