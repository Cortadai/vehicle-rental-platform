package com.vehiclerental.common.domain.entity;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class BaseEntityTest {

    // Concrete subclass for testing the abstract BaseEntity
    private static class TestEntity extends BaseEntity<UUID> {
        TestEntity(UUID id) {
            super(id);
        }
    }

    private static class OtherEntity extends BaseEntity<UUID> {
        OtherEntity(UUID id) {
            super(id);
        }
    }

    @Test
    void sameNonNullId_shouldBeEqual() {
        UUID id = UUID.randomUUID();
        var entity1 = new TestEntity(id);
        var entity2 = new TestEntity(id);

        assertThat(entity1).isEqualTo(entity2);
        assertThat(entity1.hashCode()).isEqualTo(entity2.hashCode());
    }

    @Test
    void differentIds_shouldNotBeEqual() {
        var entity1 = new TestEntity(UUID.randomUUID());
        var entity2 = new TestEntity(UUID.randomUUID());

        assertThat(entity1).isNotEqualTo(entity2);
    }

    @Test
    void nullId_shouldUseReferenceEquality() {
        var entity1 = new TestEntity(null);
        var entity2 = new TestEntity(null);

        assertThat(entity1).isEqualTo(entity1);
        assertThat(entity1).isNotEqualTo(entity2);
    }

    @Test
    void nullId_shouldReturnConstantHashCode() {
        var entity1 = new TestEntity(null);
        var entity2 = new TestEntity(null);

        assertThat(entity1.hashCode()).isEqualTo(entity2.hashCode());
    }

    @Test
    void comparedToNull_shouldReturnFalse() {
        var entity = new TestEntity(UUID.randomUUID());

        assertThat(entity).isNotEqualTo(null);
    }

    @Test
    void comparedToDifferentType_shouldReturnFalse() {
        UUID id = UUID.randomUUID();
        var entity = new TestEntity(id);
        var other = new OtherEntity(id);

        assertThat(entity).isNotEqualTo(other);
    }

    @Test
    void getId_shouldReturnTheId() {
        UUID id = UUID.randomUUID();
        var entity = new TestEntity(id);

        assertThat(entity.getId()).isEqualTo(id);
    }
}
