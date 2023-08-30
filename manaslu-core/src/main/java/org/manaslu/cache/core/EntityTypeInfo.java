package org.manaslu.cache.core;

import java.lang.reflect.Field;
import java.util.Map;

/**
 * @param rawClass  原始类型
 * @param database     数据库
 * @param table        表名
 * @param id           主键
 * @param normalFields 普通字段
 */
public record EntityTypeInfo(Class<? extends AbstractEntity<?>> rawClass, String database, String table,
                             Field id, Map<String, Field> normalFields) {
}
