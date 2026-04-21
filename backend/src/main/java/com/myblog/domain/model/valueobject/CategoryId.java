package com.myblog.domain.model.valueobject;

public class CategoryId {

    private final Long value;

    public CategoryId(Long value) {
        this.value = value;
    }

    public Long getValue() {
        return value;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof CategoryId)) {
            return false;
        }
        CategoryId other = (CategoryId) obj;
        return value != null && value.equals(other.value);
    }

    @Override
    public int hashCode() {
        return value != null ? value.hashCode() : 0;
    }
}