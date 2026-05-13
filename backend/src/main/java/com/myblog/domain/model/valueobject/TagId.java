package com.myblog.domain.model.valueobject;

/**
 * 标签 ID 値对象。
 *
 * @author my-blog
 * @since 1.0.0
 */
public class TagId {

    /** 标签 ID 的具体数字値。 */
    private final Long value;

    /**
     * 创建标签 ID 値对象。
     *
     * @param value ID 数字値
     */
    public TagId(Long value) {
        this.value = value;
    }

    /**
     * 获取 ID 的数字値。
     *
     * @return ID 数字値
     */
    public Long getValue() {
        return value;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof TagId)) {
            return false;
        }
        TagId other = (TagId) obj;
        return value != null && value.equals(other.value);
    }

    @Override
    public int hashCode() {
        return value != null ? value.hashCode() : 0;
    }
}