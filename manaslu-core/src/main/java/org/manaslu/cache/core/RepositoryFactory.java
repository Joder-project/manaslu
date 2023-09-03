package org.manaslu.cache.core;

import org.manaslu.cache.core.annotations.Entity;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.locks.StampedLock;

public class RepositoryFactory {

    private final EntityEnhanceCreator entityEnhanceCreator;
    private final DbOperatorFactory dbOperatorFactory;
    /**
     * 定时线程池
     */
    private final ScheduledExecutorService scheduledThreadPoolExecutor;

    private final Map<Class<?>, Repository<?, ?>> cache = new HashMap<>();
    private final StampedLock stampedLock = new StampedLock();

    public RepositoryFactory(EntityTypeManager entityTypeManager, DbOperatorFactory dbOperatorFactory,
                             ScheduledExecutorService scheduledThreadPoolExecutor) {
        this.entityEnhanceCreator = new EntityEnhanceCreator(entityTypeManager);
        this.dbOperatorFactory = dbOperatorFactory;
        this.scheduledThreadPoolExecutor = scheduledThreadPoolExecutor;
    }

    /**
     * 获取对应实体类的存储类
     *
     * @param entityType 实体类型
     * @param <ID>       主键
     * @param <E>        实体
     */
    @SuppressWarnings("unchecked")
    public <ID extends Comparable<ID>, E extends AbstractEntity<ID>> Repository<ID, E> getRepository(Class<E> entityType) {
        var readLock = stampedLock.readLock();
        try {
            if (cache.containsKey(entityType)) {
                return (Repository<ID, E>) cache.get(entityType);
            }
        } finally {
            stampedLock.unlockRead(readLock);
        }
        var writeLock = stampedLock.writeLock();
        try {
            if (cache.containsKey(entityType)) {
                return (Repository<ID, E>) cache.get(entityType);
            }
            var repository = createRepository(entityType);
            cache.put(entityType, repository);
            return repository;
        } finally {
            stampedLock.unlockWrite(writeLock);
        }
    }

    <ID extends Comparable<ID>, E extends AbstractEntity<ID>> Repository<ID, E> createRepository(Class<E> entityType) {
        var proxyClass = entityEnhanceCreator.getProxyClass(entityType);
        var annotation = Objects.requireNonNull(proxyClass.getAnnotation(Entity.class));
        var cache = annotation.cacheStrategy();
        var dump = annotation.dumpStrategy();
        CacheStrategy<ID, E> cacheStrategy;
        DumpStrategy<ID, E> dumpStrategy;
        switch (cache) {
            case LRU -> cacheStrategy = new LRUCacheStrategy<>(annotation.lruMaxSize(), annotation.lruMaxExpireTime());
            case PERSIST -> cacheStrategy = new PersistCacheStrategy<>();
            default -> cacheStrategy = new NoCacheStrategy<>();
        }
        var dbOperator = dbOperatorFactory.create(entityType);
        switch (dump) {
            case IMMEDIATE -> dumpStrategy = new ImmediateDumpStrategy<>(dbOperator);
            case INTERVAL ->
                    dumpStrategy = new IntervalDumpStrategy<>(annotation.intervalScheduleTime(), dbOperator, scheduledThreadPoolExecutor);
            case COUNTER -> dumpStrategy = new CountDumpStrategy<>(annotation.maxCountTriggerDump(), dbOperator);
            default ->
                    dumpStrategy = new CountIntervalDumpStrategy<>(annotation.intervalScheduleTime(), annotation.maxCountTriggerDump(), dbOperator, scheduledThreadPoolExecutor);
        }
        return new RepositoryImpl<>(cacheStrategy, dumpStrategy, entityEnhanceCreator);
    }
}
