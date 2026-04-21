package com.myblog.domain.model.valueobject;

public class TagId {

    private final Long value;

    public TagId(Long value) {
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