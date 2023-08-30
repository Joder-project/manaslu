package org.manaslu.cache.core;

final class EntityEnhanceCreator {

    private final EntityTypeManager entityTypeManager;

    EntityEnhanceCreator(EntityTypeManager entityTypeManager) {
        this.entityTypeManager = entityTypeManager;
    }

    /**
     * 创建增强实体
     */
    <ID extends Comparable<ID>, Entity extends AbstractEntity<ID>> Entity create(Entity entity, CacheStrategy<ID, ? extends AbstractEntity<ID>> cacheStrategy,
                                                                                 DumpStrategy<ID, ? extends AbstractEntity<ID>> dumpStrategy) {
        var newEnhance = entityTypeManager.newEnhance(entity);
        newEnhance.initialize(cacheStrategy, dumpStrategy);
        return newEnhance;
    }
}


