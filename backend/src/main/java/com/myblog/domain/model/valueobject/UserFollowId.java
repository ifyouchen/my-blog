package com.myblog.domain.model.valueobject;

import java.util.Objects;

/**
 * 用户关注 ID。
 *
 * @author Codex
 * @since 1.0.0
 */
public final class UserFollowId {

    /** 关注关系 ID 的数字值。 */
    private final Long value;

    /**
     * 创建用户关注 ID。
     *
     * @param value 关注 ID 原始值
     */
    public UserFollowId(Long value) {
        this.value = value;
    }

    /**
     * 获取关注 ID 原始值。
     *
     * @return 关注 ID 原始值
     */
    public Long getValue() {
        return value;
    }

    /**
     * 判断两个关注 ID 是否相等。
     *
     * @param o 待比较对象
     * @return 若相等则返回 {@code true}
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof UserFollowId)) {
            return false;
        }
        UserFollowId that = (UserFollowId) o;
        return Objects.equals(value, that.value);
    }

    /**
     * 计算关注 ID 哈希值。
     *
     * @return 哈希值
     */
    @Override
    public int hashCode() {
        return Objects.hash(value);
    }
}
