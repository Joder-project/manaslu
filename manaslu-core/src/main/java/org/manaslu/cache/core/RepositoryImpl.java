package org.manaslu.cache.core;

import lombok.extern.slf4j.Slf4j;
import org.manaslu.cache.core.exception.ManasluException;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.locks.StampedLock;

@Slf4j
final class RepositoryImpl<ID extends Comparable<ID>, E extends AbstractEntity<ID>> implements Repository<ID, E> {

    private final CacheStrategy<ID, E> cacheStrategy;
    private final DumpStrategy<ID, E> dumpStrategy;


    private final EntityEnhanceCreator entityEnhanceCreator;

    private final StampedLock lock = new StampedLock();

    RepositoryImpl(@Nonnull CacheStrategy<ID, E> cacheStrategy, @Nonnull DumpStrategy<ID, E> dumpStrategy,
                   @Nonnull EntityEnhanceCreator entityEnhanceCreator) {
        this.cacheStrategy = cacheStrategy;
        this.dumpStrategy = dumpStrategy;
        this.entityEnhanceCreator = entityEnhanceCreator;
        // 淘汰时， 刷新数据库
        this.cacheStrategy.addRemoveCallback(e -> flushToDb(e.id()));
    }

    @Nonnull
    @Override
    public List<E> listFromCache() {
        return cacheStrategy.list();
    }

    @Override
    public Optional<E> load(ID id) {
        Optional<E> e;
        var readStamp = lock.readLock();
        try {
            e = cacheStrategy.get(id);
        } finally {
            lock.unlockRead(readStamp);
        }
        if (e.isPresent()) {
            return e;
        }
        var writeLock = lock.writeLock();
        try {
            e = cacheStrategy.get(id);
            if (e.isPresent()) {
                return e;
            }
            e = dumpStrategy.select(id)
                    .map(find -> entityEnhanceCreator.create(find, cacheStrategy, dumpStrategy));
        } finally {
            lock.unlockWrite(writeLock);
        }
        return e;
    }

    @Override
    public Optional<E> loadFromDb(ID id) {
        Optional<E> e;
        var writeLock = lock.writeLock();
        try {
            e = dumpStrategy.select(id)
                    .map(find -> entityEnhanceCreator.create(find, cacheStrategy, dumpStrategy));
            if (e.isPresent()) {
                cacheStrategy.put(e.get());
            } else {
                cacheStrategy.delete(id);
            }
        } finally {
            lock.unlockWrite(writeLock);
        }
        return e;
    }

    @Override
    public E create(@Nonnull E entity) {
        var writeLock = lock.writeLock();
        E e;
        try {
            // 新增时直接入库
            var id = dumpStrategy.insert(entity);
            if (entity.id() == null && id.isPresent()) {
                e = entityEnhanceCreator.create(id.get(), entity, cacheStrategy, dumpStrategy);
            } else {
                e = entityEnhanceCreator.create(entity, cacheStrategy, dumpStrategy);
            }
            cacheStrategy.put(e);
        } catch (Exception ex) {
            log.error("新增数据错误", ex);
            throw new ManasluException(ex);
        } finally {
            lock.unlockWrite(writeLock);
        }

        return e;
    }

    @Override
    public boolean delete(ID id) {
        var writeLock = lock.writeLock();
        try {
            cacheStrategy.delete(id);
            // 删除时直接入库
            dumpStrategy.delete(id);
        } catch (Exception ex) {
            log.error("删除数据错误", ex);
            throw new ManasluException(ex);
        } finally {
            lock.unlockWrite(writeLock);
        }
        return false;
    }

    @Override
    public boolean deleteOnlyCache(ID id) {
        var writeLock = lock.writeLock();
        try {
            // 先入库
            dumpStrategy.flush(id);
            // 在删除
            cacheStrategy.delete(id);
        } finally {
            lock.unlockWrite(writeLock);
        }
        return false;
    }

    @Override
    public void flushToDb(ID id) {
        dumpStrategy.flush(id);
    }
}
