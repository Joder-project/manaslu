package org.manaslu.cache.mysql;

import org.manaslu.cache.core.AbstractEntity;
import org.manaslu.cache.core.DbOperator;
import org.manaslu.cache.core.DbOperatorFactory;
import org.manaslu.cache.core.EntityTypeManager;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.StampedLock;

public class MysqlDbOperatorFactory implements DbOperatorFactory {

    private final Map<Class<?>, DbOperator<?, ?>> cache = new HashMap<>();
    private final StampedLock stampedLock = new StampedLock();

    private final MysqlConnections connections;
    private final EntityTypeManager entityTypeManager;

    public MysqlDbOperatorFactory(MysqlConnections connections, EntityTypeManager entityTypeManager) {
        this.connections = connections;
        this.entityTypeManager = entityTypeManager;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <ID extends Comparable<ID>, E extends AbstractEntity<ID>> DbOperator<ID, E> create(Class<E> entityType) {
        var readLock = stampedLock.readLock();
        try {
            if (cache.containsKey(entityType)) {
                return (DbOperator<ID, E>) cache.get(entityType);
            }
        } finally {
            stampedLock.unlockRead(readLock);
        }
        var writeLock = stampedLock.writeLock();
        try {
            if (cache.containsKey(entityType)) {
                return (DbOperator<ID, E>) cache.get(entityType);
            }
            var dbOperator = createDbOperator(entityType);
            cache.put(entityType, dbOperator);
            return dbOperator;
        } finally {
            stampedLock.unlockWrite(writeLock);
        }
    }

    <ID extends Comparable<ID>, E extends AbstractEntity<ID>> DbOperator<ID, E> createDbOperator(Class<E> entityType) {
        return new MysqlDbOperator<>(connections, entityTypeManager.getInfo(entityType));
    }
}
