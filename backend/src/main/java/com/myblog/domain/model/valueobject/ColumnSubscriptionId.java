package com.myblog.domain.model.valueobject;

import java.util.Objects;

/**
 * 专栏订阅 ID。
 *
 * @author my-blog
 * @since 1.0.0
 */
public final class ColumnSubscriptionId {

    /** 订阅记录 ID 的数字值。 */
    private final Long value;

    /**
     * 创建专栏订阅 ID。
     *
     * @param value 订阅 ID 原始值
     */
    public ColumnSubscriptionId(Long value) {
        this.value = value;
    }

    /**
     * 获取订阅 ID 原始值。
     *
     * @return 订阅 ID 原始值
     */
    public Long getValue() {
        return value;
    }

    /**
     * 判断两个订阅 ID 是否相等。
     *
     * @param o 待比较对象
     * @return 若相等则返回 {@code true}
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ColumnSubscriptionId)) {
            return false;
        }
        ColumnSubscriptionId that = (ColumnSubscriptionId) o;
        return Objects.equals(value, that.value);
    }

    /**
     * 计算订阅 ID 哈希值。
     *
     * @return 哈希值
     */
    @Override
    public int hashCode() {
        return Objects.hash(value);
    }
}
