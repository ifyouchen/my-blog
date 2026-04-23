package com.myblog.domain.model.valueobject;

import java.util.Objects;

/**
 * 用户关注 ID。
 *
 * @author Codex
 * @since 1.0.0
 */
public final class UserFollowId {

    private final Long value;

    public UserFollowId(Long value) {
        this.value = value;
    }

    public Long getValue() {
        return value;
    }

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

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }
}
