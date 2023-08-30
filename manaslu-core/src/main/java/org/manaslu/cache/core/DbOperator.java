package org.manaslu.cache.core;

import java.util.Optional;

/**
 * 数据库操作
 */
public interface DbOperator<ID extends Comparable<ID>, Entity extends AbstractEntity<ID>> {

    Optional<Entity> select(ID id);

    void insert(Entity entity);

    void update(UpdateInfo<ID, Entity> entity);

    void delete(ID id);
}
