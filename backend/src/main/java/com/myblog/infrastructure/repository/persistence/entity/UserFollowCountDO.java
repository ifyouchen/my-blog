package com.myblog.infrastructure.repository.persistence.entity;

import java.util.Objects;

/**
 * 用户关注数量聚合数据对象。
 *
 * @author Codex
 * @since 1.0.0
 */
public class UserFollowCountDO {

    /**
     * 用户 ID
     */
    private Long userId;

    /**
     * 关注或粉丝数量
     */
    private Integer cnt;

    /**
     * 获取用户 ID。
     *
     * @return 用户 ID
     */
    public Long getUserId() {
        return userId;
    }

    /**
     * 设置用户 ID。
     *
     * @param userId 用户 ID
     */
    public void setUserId(Long userId) {
        this.userId = userId;
    }

    /**
     * 获取关注或粉丝数量。
     *
     * @return 数量
     */
    public Integer getCnt() {
        return cnt;
    }

    /**
     * 设置关注或粉丝数量。
     *
     * @param cnt 数量
     */
    public void setCnt(Integer cnt) {
        this.cnt = cnt;
    }

    /**
     * 判断两个对象是否相等（以 userId 为准）。
     *
     * @param o 待比较对象
     * @return 若相等则返回 {@code true}
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UserFollowCountDO that = (UserFollowCountDO) o;
        return Objects.equals(userId, that.userId);
    }

    /**
     * 计算哈希值（以 userId 为准）。
     *
     * @return 哈希值
     */
    @Override
    public int hashCode() {
        return Objects.hash(userId);
    }
}