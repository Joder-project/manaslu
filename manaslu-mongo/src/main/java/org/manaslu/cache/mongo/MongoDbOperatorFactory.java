package org.manaslu.cache.mongo;

import com.mongodb.client.MongoClient;
import org.manaslu.cache.core.AbstractEntity;
import org.manaslu.cache.core.DbOperator;
import org.manaslu.cache.core.DbOperatorFactory;
import org.manaslu.cache.core.EntityTypeManager;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.StampedLock;

public class MongoDbOperatorFactory implements DbOperatorFactory {

    private final Map<Class<?>, DbOperator<?, ?>> cache = new HashMap<>();
    private final StampedLock stampedLock = new StampedLock();

    private final MongoClient mongoClient;
    private final EntityTypeManager entityTypeManager;

    private final String defaultDatabase;

    public MongoDbOperatorFactory(String defaultDatabase, MongoClient mongoClient, EntityTypeManager entityTypeManager) {
        this.defaultDatabase = defaultDatabase;
        this.mongoClient = mongoClient;
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
        return new MongoDbOperator<>(mongoClient, defaultDatabase, entityTypeManager.getInfo(entityType));
    }
}
