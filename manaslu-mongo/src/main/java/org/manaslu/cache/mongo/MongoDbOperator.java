package org.manaslu.cache.mongo;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.manaslu.cache.core.AbstractEntity;
import org.manaslu.cache.core.DbOperator;
import org.manaslu.cache.core.EntityTypeInfo;
import org.manaslu.cache.core.UpdateInfo;
import org.manaslu.cache.core.exception.ManasluException;

import javax.annotation.Nonnull;
import java.util.Optional;
import java.util.stream.StreamSupport;

/**
 * mongo操作实现类
 */
public final class MongoDbOperator<ID extends Comparable<ID>, Entity extends AbstractEntity<ID>> implements DbOperator<ID, Entity> {

    private final MongoCollection<Document> collection;

    private final MongoEntityInfo entityTypeInfo;

    public MongoDbOperator(@Nonnull MongoClient client, String defaultDatabase, @Nonnull EntityTypeInfo entityTypeInfo) {
        var database = client.getDatabase(Optional.ofNullable(entityTypeInfo.database()).orElse(defaultDatabase));
        this.collection = database.getCollection(entityTypeInfo.table());
        this.entityTypeInfo = new MongoEntityInfo(entityTypeInfo);
    }

    @Override
    public Optional<Entity> select(ID id) {
        return StreamSupport.stream(collection.find(Filters.eq("_id", id)).spliterator(), false)
                .map(this::toEntity)
                .findFirst();
    }

    @SuppressWarnings("unchecked")
    @Override
    public Optional<ID> insert(@Nonnull Entity entity) {
        ID id;
        // 只支持ObjectId
        if (entity.id() == null && entityTypeInfo.entityTypeInfo.id().getType().equals(ObjectId.class)) {
            id = (ID) new ObjectId();
        } else {
            id = entity.id();
        }
        var document = toDocument(id, entity);
        collection.insertOne(document);
        return Optional.ofNullable(id);
    }

    @Override
    public void update(@Nonnull UpdateInfo<ID, Entity> entity) {
        collection.updateOne(Filters.eq("_id", entity.entity().id()),
                toUpdateDocument(entity));
    }

    @Override
    public void delete(ID id) {
        collection.deleteOne(Filters.eq("_id", id));
    }

    @SuppressWarnings("unchecked")
    Entity toEntity(Document document) {
        var entity = (Entity) entityTypeInfo.createInstance();
        try {
            entityTypeInfo.entityTypeInfo.id().set(entity, document.get("_id"));
        } catch (Exception ex) {
            throw new ManasluException("设置主键失败", ex);
        }
        entityTypeInfo.entityTypeInfo.normalFields().forEach((k, v) -> {
            try {
                v.set(entity, document.get(k, v.getType()));
            } catch (Exception ex) {
                throw new ManasluException("设置属性失败", ex);
            }
        });
        return entity;
    }

    Document toDocument(ID id, Entity entity) {
        var document = new Document();
        document.put("_id", id);
        entityTypeInfo.entityTypeInfo.normalFields().forEach((k, v) -> {
            try {
                document.put(k, v.get(entity));
            } catch (Exception ex) {
                throw new ManasluException("获取属性失败", ex);
            }
        });
        return document;
    }

    Document toUpdateDocument(UpdateInfo<ID, Entity> info) {
        var document = new Document();
        entityTypeInfo.entityTypeInfo.normalFields().forEach((k, v) -> {
            if (info.updateProperties().contains(k)) {
                try {
                    document.put(k, v.get(info.entity()));
                } catch (Exception ex) {
                    throw new ManasluException("获取属性失败", ex);
                }
            }
        });
        return new Document("$set", document);
    }

    record MongoEntityInfo(EntityTypeInfo entityTypeInfo) {

        @SuppressWarnings("unchecked")
        <ID extends Comparable<ID>, E extends AbstractEntity<ID>> E createInstance() {
            try {
                return (E) entityTypeInfo.rawClass().getDeclaredConstructor().newInstance();
            } catch (Exception ex) {
                throw new ManasluException("新建对象失败", ex);
            }
        }
    }
}
