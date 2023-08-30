package org.manaslu.cache.core;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * 缓存策略
 */
public sealed interface CacheStrategy<ID extends Comparable<ID>, Entity extends AbstractEntity<ID>> permits LRUCacheStrategy, NoCacheStrategy {

    /**
     * 添加或者更新数据
     *
     * @param entity 数据
     */
    void put(Entity entity);

    /**
     * 删除缓存
     */
    void delete(ID id);

    /**
     * 查看缓存数据
     *
     * @return
     */
    List<Entity> list();

    /**
     * 查询数据
     *
     * @param id ID
     * @return 数据
     */
    Optional<Entity> get(ID id);

    /**
     * 实体卸载是回调
     */
    void addRemoveCallback(Consumer<Entity> supplier);
}

/**
 * 采用lru淘汰策略
 */
sealed class LRUCacheStrategy<ID extends Comparable<ID>, Entity extends AbstractEntity<ID>> implements CacheStrategy<ID, Entity> permits PersistCacheStrategy {

    private final Cache<ID, Entity> cache;

    private final LinkedBlockingQueue<Consumer<Entity>> callbacks = new LinkedBlockingQueue<>();

    /**
     * @param maxSize        最大数量
     * @param maxExpiredTime 最大超时时间(ms), -1 代表永久
     */
    LRUCacheStrategy(int maxSize, long maxExpiredTime) {
        var builder = CacheBuilder.newBuilder().maximumSize(maxSize);
        if (maxExpiredTime > 0) {
            builder.expireAfterAccess(maxExpiredTime, TimeUnit.MILLISECONDS);
        }
        this.cache = builder.<ID, Entity>removalListener(notification -> {
                    var value = notification.getValue();
                    callbacks.forEach(e -> e.accept(value));
                })
                .build();
    }

    @Override
    public void put(Entity entity) {
        cache.put(entity.id(), entity);
    }

    @Override
    public void delete(ID id) {
        cache.invalidate(id);
    }

    @Override
    public List<Entity> list() {
        return cache.asMap()
                .values()
                .stream()
                .toList();
    }

    @Override
    public Optional<Entity> get(ID id) {
        return Optional.ofNullable(cache.getIfPresent(id));
    }

    @Override
    public void addRemoveCallback(Consumer<Entity> consumer) {
        callbacks.add(consumer);
    }
}

/**
 * 持久化，不采用淘汰策略
 */
final class PersistCacheStrategy<ID extends Comparable<ID>, Entity extends AbstractEntity<ID>> extends LRUCacheStrategy<ID, Entity> {

    PersistCacheStrategy() {
        super(Integer.MAX_VALUE, -1);
    }

}

/**
 * 不采用缓存
 */
final class NoCacheStrategy<ID extends Comparable<ID>, Entity extends AbstractEntity<ID>> implements CacheStrategy<ID, Entity> {
    @Override
    public void put(Entity entity) {

    }

    @Override
    public void delete(ID id) {

    }

    @Override
    public List<Entity> list() {
        return Collections.emptyList();
    }

    @Override
    public Optional<Entity> get(ID id) {
        return Optional.empty();
    }

    @Override
    public void addRemoveCallback(Consumer<Entity> supplier) {

    }
}
