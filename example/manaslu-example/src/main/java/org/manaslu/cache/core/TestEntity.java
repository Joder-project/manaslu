package org.manaslu.cache.core;

import java.util.List;

public class TestEntity {

    public static void main(String[] args) {
        var userEntityClass = UserEntity.class;
        var raw = new UserEntity();
        var entityTypeManager = new EntityTypeManager();
        entityTypeManager.registerTypes(List.of(UserEntity.class));
        var proxy = entityTypeManager.newEnhance(1, raw);
        assert proxy.add(1, 1, null) == raw.add(1, 1, null);
    }
}
