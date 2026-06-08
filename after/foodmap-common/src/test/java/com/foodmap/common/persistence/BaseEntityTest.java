package com.foodmap.common.persistence;

import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class BaseEntityTest {

    @Test
    void storesFixedTableFieldsWithoutBusinessKey() {
        DemoEntity entity = new DemoEntity();
        OffsetDateTime createdTime = OffsetDateTime.parse("2026-06-08T10:00:00+08:00");
        OffsetDateTime updatedTime = OffsetDateTime.parse("2026-06-08T11:00:00+08:00");

        entity.setId(1L);
        entity.setCreatedTime(createdTime);
        entity.setUpdatedTime(updatedTime);
        entity.setIsDelete((short) 0);

        assertThat(entity.getId()).isEqualTo(1L);
        assertThat(entity.getCreatedTime()).isEqualTo(createdTime);
        assertThat(entity.getUpdatedTime()).isEqualTo(updatedTime);
        assertThat(entity.getIsDelete()).isZero();
    }

    private static final class DemoEntity extends BaseEntity {
    }
}
