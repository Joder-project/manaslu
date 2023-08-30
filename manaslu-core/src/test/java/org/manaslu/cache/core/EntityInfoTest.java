package org.manaslu.cache.core;

import org.junit.jupiter.api.Test;

import java.util.List;

class EntityInfoTest {

    @Test
    void buildProxy() {
        var entityTypeManager = new EntityTypeManager(List.of(UserEntity.class));
        var userEntity = new UserEntity();
        var userEntity1 = entityTypeManager.newEnhance(userEntity);
    }
}