package org.manaslu.cache.mysql;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.manaslu.cache.core.AbstractEntity;
import org.manaslu.cache.core.DbOperator;
import org.manaslu.cache.core.EntityTypeInfo;
import org.manaslu.cache.core.UpdateInfo;
import org.manaslu.cache.core.exception.ManasluException;

import javax.annotation.Nonnull;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Slf4j
public class MysqlDbOperator<ID extends Comparable<ID>, Entity extends AbstractEntity<ID>> implements DbOperator<ID, Entity> {
    static final ObjectMapper MAPPER = new ObjectMapper();
    private final MysqlConnections connections;
    private final MysqlEntityInfo entityInfo;
    private final String tableName;
    private final String selectSql;
    private final String insertSql;

    private final String deleteSql;

    public MysqlDbOperator(@Nonnull MysqlConnections connections, @Nonnull EntityTypeInfo entityTypeInfo) {
        this.connections = connections;
        this.entityInfo = new MysqlEntityInfo(entityTypeInfo);
        this.tableName = entityTypeInfo.database() == null ? entityTypeInfo.table() : entityTypeInfo.database() + "." + entityTypeInfo.table();
        this.selectSql = String.format("select * from `%s` where `id` = ?;", tableName);
        this.deleteSql = String.format("delete from `%s` where `id` = ?;", tableName);
        this.insertSql = buildInsert();
    }

    String buildInsert() {
        StringBuilder sb = new StringBuilder("insert into `").append(tableName).append("` (");
        var properties = entityInfo.propertyTypes;
        boolean first = true;
        for (var property : properties) {
            if (!first) {
                sb.append(", ");
            }
            sb.append("`").append(property.name()).append("`");
            first = false;
        }
        sb.append(") values (");
        for (int i = 0; i < properties.size(); i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append("?");
        }
        sb.append(");");
        return sb.toString();
    }

    @Override
    public Optional<Entity> select(ID id) {
        try (var connection = connections.getConnection()) {
            var preparedStatement = connection.prepareStatement(selectSql);
            preparedStatement.setObject(1, id);
            var resultSet = preparedStatement.executeQuery();
            if (resultSet.next()) {
                return Optional.of(toEntity(resultSet));
            }
        } catch (SQLException e) {
            throw new ManasluException("执行SQL失败", e);
        } finally {
            log.debug("select SQL = {}, id = {}", selectSql, id);
        }
        return Optional.empty();
    }

    @Override
    public Optional<ID> insert(@Nonnull Entity entity) {
        try (var connection = connections.getConnection()) {
            var preparedStatement = connection.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS);
            for (int i = 0; i < entityInfo.propertyTypes.size(); i++) {
                var key = entityInfo.propertyTypes.get(i).name;
                if (entityInfo.entityTypeInfo.normalFields().containsKey(key)) {
                    var field = entityInfo.entityTypeInfo.normalFields().get(key);
                    var object = field.get(entity);
                    if (entityInfo.entityTypeInfo.subEntities().containsKey(field.getType())) {
                        preparedStatement.setObject(i + 1, object == null ? "" : MAPPER.writeValueAsString(object));
                    } else {
                        preparedStatement.setObject(i + 1, object);
                    }
                } else if ("id".equals(key)) {
                    preparedStatement.setObject(i + 1, entity.id());
                }
            }
            preparedStatement.executeUpdate();
            var resultSet = preparedStatement.getGeneratedKeys();
            if (resultSet.next()) {
                return toID(entityInfo.entityTypeInfo.id().getType(), resultSet);
            }
            return Optional.empty();
        } catch (SQLException e) {
            throw new ManasluException("执行SQL失败", e);
        } catch (IllegalAccessException e) {
            throw new ManasluException("获取属性失败", e);
        } catch (Exception e) {
            throw new ManasluException("出现异常", e);
        } finally {
            log.debug("insert SQL = {}, entity = {}", insertSql, entity);
        }
    }

    @SuppressWarnings("unchecked")
    private Optional<ID> toID(Class<?> clazz, ResultSet resultSet) {
        try {
            if (clazz.equals(Short.class) || clazz.equals(short.class)) {
                return Optional.of((ID) Short.valueOf(resultSet.getShort(1)));
            } else if (clazz.equals(Integer.class) || clazz.equals(int.class)) {
                return Optional.of((ID) Integer.valueOf(resultSet.getInt(1)));
            } else if (clazz.equals(Long.class) || clazz.equals(long.class)) {
                return Optional.of((ID) Long.valueOf(resultSet.getLong(1)));
            }
        } catch (Exception ex) {
            log.error("获取主键失败", ex);
        }
        return Optional.empty();
    }


    @Override
    public void update(@Nonnull UpdateInfo<ID, Entity> entity) {
        final var sql = buildUpdate(entity);
        try (var connection = connections.getConnection()) {
            var preparedStatement = connection.prepareStatement(sql);
            int i = 1;
            for (var propertyType : entityInfo.propertyTypes) {
                var key = propertyType.name;
                if (!entity.updateProperties().contains(key)) {
                    continue;
                }
                if (entityInfo.entityTypeInfo.normalFields().containsKey(key)) {
                    var field = entityInfo.entityTypeInfo.normalFields().get(key);
                    var object = field.get(entity.entity());
                    if (entityInfo.entityTypeInfo.subEntities().containsKey(field.getType())) {
                        preparedStatement.setObject(i, object == null ? "" : MAPPER.writeValueAsString(object));
                    } else {
                        preparedStatement.setObject(i, object);
                    }
                    i++;
                }
            }
            preparedStatement.setObject(i, entity.entity().id());
            preparedStatement.executeUpdate();
        } catch (SQLException e) {
            throw new ManasluException("执行SQL失败", e);
        } catch (IllegalAccessException e) {
            throw new ManasluException("获取属性失败", e);
        } catch (Exception e) {
            throw new ManasluException("出现异常", e);
        } finally {
            log.debug("update SQL = {}, entity = {}", sql, entity.entity());
        }
    }

    String buildUpdate(UpdateInfo<ID, Entity> entity) {
        StringBuilder sb = new StringBuilder("update `").append(tableName).append("` set ");
        var properties = entityInfo.propertyTypes;
        boolean first = true;
        for (var property : properties) {
            if (!entity.updateProperties().contains(property.name())) {
                continue;
            }
            if (!first) {
                sb.append(", ");
            }
            sb.append("`").append(property.name()).append("` = ?");
            first = false;
        }
        sb.append(" where `id` = ?");
        return sb.toString();
    }


    @Override
    public void delete(ID id) {
        try (var connection = connections.getConnection()) {
            var preparedStatement = connection.prepareStatement(deleteSql);
            preparedStatement.setObject(1, id);
            preparedStatement.execute();
        } catch (SQLException e) {
            throw new ManasluException("执行SQL失败", e);
        } finally {
            log.debug("delete SQL = {}, id = {}", deleteSql, id);
        }
    }

    @SuppressWarnings("unchecked")
    Entity toEntity(ResultSet resultSet) {
        var entity = (Entity) entityInfo.createInstance();
        try {
            entityInfo.entityTypeInfo.id().set(entity, resultSet.getObject("id", entityInfo.entityTypeInfo.id().getType()));
        } catch (Exception ex) {
            throw new ManasluException("设置主键失败", ex);
        }
        entityInfo.entityTypeInfo.normalFields().forEach((k, v) -> {
            try {
                if (entityInfo.entityTypeInfo.subEntities().containsKey(v.getType())) {
                    var json = resultSet.getString(k);
                    if (json == null || json.isEmpty()) {
                        v.set(entity, null);
                        return;
                    }
                    v.set(entity, toObject(MAPPER.readTree(json), v.getType()));
                } else {
                    v.set(entity, resultSet.getObject(k, v.getType()));
                }
            } catch (Exception ex) {
                throw new ManasluException("设置属性失败", ex);
            }
        });
        return entity;
    }

    Object toObject(JsonNode document, Class<?> clazz) throws Exception {
        var info = entityInfo.entityTypeInfo.subEntities().get(clazz);
        Object object = clazz.getDeclaredConstructor().newInstance();
        info.fields().forEach((k, v) -> {
            try {
                if (entityInfo.entityTypeInfo.subEntities().containsKey(v.getType())) {
                    v.set(object, toObject(document.get(k), v.getType()));
                } else {
                    v.set(object, MAPPER.readValue(document.get(k).toString(), v.getType()));
                }
            } catch (Exception ex) {
                throw new ManasluException("设置属性失败", ex);
            }
        });
        return object;
    }

    static class MysqlEntityInfo {

        final EntityTypeInfo entityTypeInfo;

        final List<Wrapper> propertyTypes;

        MysqlEntityInfo(EntityTypeInfo entityTypeInfo) {
            this.entityTypeInfo = entityTypeInfo;
            this.propertyTypes = buildType();
        }

        @SuppressWarnings("unchecked")
        <ID extends Comparable<ID>, E extends AbstractEntity<ID>> E createInstance() {
            try {
                return (E) entityTypeInfo.rawClass().getDeclaredConstructor().newInstance();
            } catch (Exception ex) {
                throw new ManasluException("新建对象失败", ex);
            }
        }

        List<Wrapper> buildType() {
            var wrappers = new ArrayList<Wrapper>();
            wrappers.add(new Wrapper(entityTypeInfo.id().getName()));
            entityTypeInfo.normalFields().keySet().forEach(n -> wrappers.add(new Wrapper(n)));
            return Collections.unmodifiableList(wrappers);
        }

        record Wrapper(String name) {

        }
    }
}
