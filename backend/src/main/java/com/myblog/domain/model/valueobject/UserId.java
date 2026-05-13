package com.myblog.domain.model.valueobject;

import java.io.Serializable;
import java.util.Objects;

/**
 * 用户 ID 值对象。
 *
 * @author my-blog
 * @since 1.0.0
 */
public class UserId implements Serializable {

    private final Long value;

    /**
     * 创建用户 ID。
     *
     * @param value 用户 ID 原始值
     */
    public UserId(Long value) {
        if (value == null || value.longValue() <= 0L) {
            throw new IllegalArgumentException("用户ID不能为空");
        }
        this.value = value;
    }

    /**
     * 获取用户 ID 原始值。
     *
     * @return 用户 ID 原始值
     */
    public Long getValue() {
        return value;
    }

    /**
     * 判断两个用户 ID 是否相同。
     *
     * @param o 待比较对象
     * @return 是否相同
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof UserId)) {
            return false;
        }
        UserId userId = (UserId) o;
        return Objects.equals(value, userId.value);
    }

    /**
     * 计算用户 ID 哈希值。
     *
     * @return 哈希值
     */
    @Override
    public int hashCode() {
        return Objects.hash(value);
    }
}
