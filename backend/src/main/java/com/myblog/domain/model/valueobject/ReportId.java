package com.myblog.domain.model.valueobject;

import java.io.Serializable;
import java.util.Objects;

/**
 * 举报 ID 值对象。
 *
 * @author Codex
 * @since 1.0.0
 */
public class ReportId implements Serializable {

    private final Long value;

    public ReportId(Long value) {
        if (value == null || value.longValue() <= 0L) {
            throw new IllegalArgumentException("举报ID不能为空");
        }
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
        if (!(o instanceof ReportId)) {
            return false;
        }
        ReportId reportId = (ReportId) o;
        return Objects.equals(value, reportId.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }
}
