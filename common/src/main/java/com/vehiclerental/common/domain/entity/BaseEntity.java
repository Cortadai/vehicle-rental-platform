package com.vehiclerental.common.domain.entity;

import java.util.Objects;

public abstract class BaseEntity<ID> {

    private final ID id;

    protected BaseEntity(ID id) {
        this.id = id;
    }

    public ID getId() {
        return id;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (id == null) return false;
        BaseEntity<?> that = (BaseEntity<?>) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        if (id == null) return 0;
        return Objects.hashCode(id);
    }
}
