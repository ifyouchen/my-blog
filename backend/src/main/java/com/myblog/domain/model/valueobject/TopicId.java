package com.myblog.domain.model.valueobject;

import java.util.Objects;

/**
 * 专题 ID。
 *
 * @author Codex
 * @since 1.0.0
 */
public final class TopicId {

    /** 专题 ID 的数字值。 */
    private final Long value;

    /**
     * 创建专题 ID。
     *
     * @param value 专题 ID 原始值
     */
    public TopicId(Long value) {
        this.value = value;
    }

    /**
     * 获取专题 ID 原始值。
     *
     * @return 专题 ID 原始值
     */
    public Long getValue() {
        return value;
    }

    /**
     * 判断两个专题 ID 是否相等。
     *
     * @param o 待比较对象
     * @return 若相等则返回 {@code true}
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof TopicId)) {
            return false;
        }
        TopicId topicId = (TopicId) o;
        return Objects.equals(value, topicId.value);
    }

    /**
     * 计算专题 ID 哈希值。
     *
     * @return 哈希值
     */
    @Override
    public int hashCode() {
        return Objects.hash(value);
    }
}
