package com.myblog.domain.model.valueobject;

import java.util.Objects;

/**
 * 专题 ID。
 *
 * @author Codex
 * @since 1.0.0
 */
public final class TopicId {

    private final Long value;

    public TopicId(Long value) {
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
        if (!(o instanceof TopicId)) {
            return false;
        }
        TopicId topicId = (TopicId) o;
        return Objects.equals(value, topicId.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }
}
