package org.manaslu.cache.core;

import java.lang.reflect.Field;
import java.util.Map;

/**
 * @param rawClass  原始类型
 * @param database     数据库
 * @param table        表名
 * @param id           主键
 * @param normalFields 普通字段
 * @param subEntities 所有子实体
 */
public record EntityTypeInfo(Class<? extends AbstractEntity<?>> rawClass, String database, String table,
                             Field id, Map<String, ManasluField> normalFields,
                             Map<Class<?>, SubEntityTypeInfo> subEntities) {

    public record SubEntityTypeInfo(Class<?> rawClass, Map<String, ManasluField> fields) {
    }
}
