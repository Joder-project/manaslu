package org.manaslu.cache.core;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Optional;

/**
 * 获取数据接
 *
 * @param <ID> 主键
 * @param <E>  对应实体
 */
public sealed interface Repository<ID extends Comparable<ID>, E extends AbstractEntity<ID>> permits RepositoryImpl {

    /**
     * 缓存中数据
     */
    @Nonnull
    List<E> listFromCache();

    /**
     * 取数据
     * 首先从缓存取，没有会去查询数据库
     *
     * @param id 主键
     * @return 对应的值
     */
    Optional<E> load(ID id);

    /**
     * 取数据
     * 强制从数据库取，在刷新缓存
     *
     * @param id 主键
     * @return 对应的值
     */
    Optional<E> loadFromDb(ID id);

    /**
     * 创建实体
     */
    E create(@Nonnull E entity);

    /**
     * 删除数据
     */
    void delete(ID id);

    /**
     * 只删除缓存数据，删除前会更新到数据库
     */
    void deleteOnlyCache(ID id);

    /**
     * 立即入库
     */
    void flushToDb(ID id);
}
