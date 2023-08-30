package org.manaslu.cache.core;

import javax.annotation.Nonnull;
import java.util.Optional;

/**
 * 数据库操作
 */
public interface DbOperator<ID extends Comparable<ID>, Entity extends AbstractEntity<ID>> {

    Optional<Entity> select(ID id);

    Optional<ID> insert(@Nonnull Entity entity);

    void update(@Nonnull UpdateInfo<ID, Entity> entity);

    void delete(ID id);
}
