package org.manaslu.cache.core;

import lombok.extern.slf4j.Slf4j;
import org.manaslu.cache.core.exception.ManasluException;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.StampedLock;

/**
 * 存储策略
 */
public sealed interface DumpStrategy<ID extends Comparable<ID>, Entity extends AbstractEntity<ID>> permits AbstractDumpStrategy, NoDumpStrategy {

    void update(UpdateInfo<ID, Entity> info);

    /**
     * 直接刷新
     */
    void flush(ID id);

    Optional<Entity> select(ID id);

    Optional<ID> insert(Entity entity);

    /**
     * 删除数据库数据，同时移除本次所有更新
     */
    void delete(ID id);
}

/**
 * 不采用存储
 */
final class NoDumpStrategy<ID extends Comparable<ID>, Entity extends AbstractEntity<ID>> implements DumpStrategy<ID, Entity> {

    @Override
    public void update(UpdateInfo<ID, Entity> info) {

    }

    @Override
    public void flush(ID id) {

    }

    @Override
    public Optional<Entity> select(ID id) {
        return Optional.empty();
    }

    @Override
    public Optional<ID> insert(Entity entity) {
        return Optional.empty();
    }

    @Override
    public void delete(ID id) {

    }
}

abstract sealed class AbstractDumpStrategy<ID extends Comparable<ID>, Entity extends AbstractEntity<ID>> implements DumpStrategy<ID, Entity>
        permits DelayDumpStrategy, ImmediateDumpStrategy {

    protected final DbOperator<ID, Entity> dbOperator;

    protected AbstractDumpStrategy(DbOperator<ID, Entity> dbOperator) {
        this.dbOperator = dbOperator;
    }

    @Override
    public Optional<Entity> select(ID id) {
        return dbOperator.select(id);
    }

    @Override
    public Optional<ID> insert(Entity entity) {
        return dbOperator.insert(entity);
    }
}

/**
 * 立即存储
 */
@Slf4j
final class ImmediateDumpStrategy<ID extends Comparable<ID>, Entity extends AbstractEntity<ID>> extends AbstractDumpStrategy<ID, Entity> {
    ImmediateDumpStrategy(DbOperator<ID, Entity> dbOperator) {
        super(dbOperator);
    }

    @Override
    public void update(UpdateInfo<ID, Entity> info) {
        try {
            dbOperator.update(info);
        } catch (Exception e) {
            log.error("数据入库异常", e);
            throw new ManasluException(e);
        }

    }

    @Override
    public void flush(ID id) {
        // do nothing
    }

    @Override
    public void delete(ID id) {
        try {
            dbOperator.delete(id);
        } catch (Exception e) {
            log.error("数据入库异常", e);
            throw new ManasluException(e);
        }
    }
}

/**
 * 延迟更新策略
 */
@Slf4j
sealed abstract class DelayDumpStrategy<ID extends Comparable<ID>, Entity extends AbstractEntity<ID>> extends AbstractDumpStrategy<ID, Entity>
        permits IntervalDumpStrategy, CountDumpStrategy {
    final Map<ID, UpdateInfo<ID, Entity>> cache = new HashMap<>();
    final StampedLock lock = new StampedLock();

    protected DelayDumpStrategy(DbOperator<ID, Entity> dbOperator) {
        super(dbOperator);
        // 系统结束时落库
        Runtime.getRuntime().addShutdownHook(new Thread(this::flushAll));
    }

    @Override
    public void update(UpdateInfo<ID, Entity> info) {
        var writeLock = lock.writeLock();
        try {
            var id = info.entity().id();
            if (cache.containsKey(id)) {
                var merge = cache.get(id).merge(info);
                cache.put(id, merge);
            } else {
                cache.put(id, info);
            }
        } finally {
            lock.unlockWrite(writeLock);
        }
    }

    @Override
    public void flush(ID id) {
        var writeLock = lock.writeLock();
        UpdateInfo<ID, Entity> remove = null;
        try {
            remove = cache.remove(id);
            if (remove == null) {
                return;
            }
            dbOperator.update(remove);
        } catch (Exception e) {
            if (remove != null) {
                cache.put(id, remove);
            }
            log.error("数据入库异常", e);
            throw new ManasluException(e);
        } finally {
            lock.unlockWrite(writeLock);
        }
    }

    @Override
    public void delete(ID id) {
        var writeLock = lock.writeLock();
        var remove = cache.remove(id);
        try {
            dbOperator.delete(id);
        } catch (Exception ex) {
            log.error("删除数据库数据失败", ex);
            if (remove != null) {
                cache.put(id, remove);
            }
            throw new ManasluException(ex);
        } finally {
            lock.unlockWrite(writeLock);
        }
    }

    void flushAll() {
        var list = cache.keySet().stream().toList();
        for (ID id : list) {
            flush(id);
        }
    }
}

/**
 * 定时触发存储
 */
@Slf4j
sealed class IntervalDumpStrategy<ID extends Comparable<ID>, Entity extends AbstractEntity<ID>> extends DelayDumpStrategy<ID, Entity>
        permits CountIntervalDumpStrategy {

    /**
     * @param intervalTimeMs     间隔时间
     * @param threadPoolExecutor 定时器
     */
    IntervalDumpStrategy(long intervalTimeMs, DbOperator<ID, Entity> dbOperator, ScheduledExecutorService threadPoolExecutor) {
        super(dbOperator);
        threadPoolExecutor.schedule(this::flushAll, intervalTimeMs, TimeUnit.MILLISECONDS);
    }

}

/**
 * 当满足达到更新数量或者定时时，触发存储
 */
@Slf4j
final class CountIntervalDumpStrategy<ID extends Comparable<ID>, Entity extends AbstractEntity<ID>> extends IntervalDumpStrategy<ID, Entity> {

    final int maxSize;
    final AtomicInteger counter = new AtomicInteger(0);

    /**
     * @param maxSize 最大数量
     */
    CountIntervalDumpStrategy(long intervalTimeMs, int maxSize, DbOperator<ID, Entity> dbOperator, ScheduledExecutorService threadPoolExecutor) {
        super(intervalTimeMs, dbOperator, threadPoolExecutor);
        this.maxSize = maxSize;
    }

    @Override
    public void update(UpdateInfo<ID, Entity> info) {
        super.update(info);
        // 触发入库
        if (counter.get() >= maxSize) {
            flushAll();
        }
    }

    @Override
    void flushAll() {
        super.flushAll();
        counter.set(0);
    }
}

/**
 * 当满足达到更新数量触发存储
 */
@Slf4j
final class CountDumpStrategy<ID extends Comparable<ID>, Entity extends AbstractEntity<ID>> extends DelayDumpStrategy<ID, Entity> {
    final int maxSize;
    final AtomicInteger counter = new AtomicInteger(0);

    /**
     * @param maxSize 最大数量
     */
    CountDumpStrategy(int maxSize, DbOperator<ID, Entity> dbOperator) {
        super(dbOperator);
        this.maxSize = maxSize;
    }

    @Override
    public void update(UpdateInfo<ID, Entity> info) {
        super.update(info);
        // 触发入库
        if (counter.get() >= maxSize) {
            flushAll();
        }
    }

    @Override
    void flushAll() {
        super.flushAll();
        counter.set(0);
    }
}

