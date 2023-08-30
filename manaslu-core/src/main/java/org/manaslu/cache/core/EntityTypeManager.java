package org.manaslu.cache.core;

import org.manaslu.cache.core.annotations.Entity;
import org.manaslu.cache.core.annotations.Id;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.UndeclaredThrowableException;
import java.util.*;
import java.util.stream.Collectors;

public class EntityTypeManager {

    private final Map<Class<?>, EntityInfo> registerTypes = new HashMap<>();

    public void registerTypes(List<Class<? extends AbstractEntity<?>>> registerClasses) {
        var map = new HashMap<Class<?>, EntityInfo>();
        for (Class<? extends AbstractEntity<?>> clazz : registerClasses) {
            if (map.containsKey(clazz)) {
                throw new IllegalStateException("出现重复类型" + clazz.getName());
            }
            map.put(clazz, new EntityInfo(clazz));
        }
        this.registerTypes.putAll(map);
    }

    /**
     * 获取增强对象
     */
    @SuppressWarnings("unchecked")
    <ID extends Comparable<ID>, E extends AbstractEntity<ID>> E newEnhance(ID id, E entity) {
        if (!registerTypes.containsKey(entity.getClass())) {
            throw new IllegalArgumentException("出现为注册类型" + entity.getClass().getName());
        }
        var entityInfo = registerTypes.get(entity.getClass());
        return (E) entityInfo.newObject(entity, id);
    }

    /**
     * 获取类信息
     */
    public EntityTypeInfo getInfo(Class<? extends AbstractEntity<?>> type) {
        if (!registerTypes.containsKey(type)) {
            throw new IllegalArgumentException("出现为注册类型" + type.getName());
        }
        var entityInfo = registerTypes.get(type);
        return new EntityTypeInfo(entityInfo.rawClass, entityInfo.database, entityInfo.table,
                entityInfo.idField,
                entityInfo.properties.stream().collect(Collectors.toUnmodifiableMap(Field::getName, e -> e)));
    }

    /**
     * 类型对应信息
     */
    static class EntityInfo {

        final Class<? extends AbstractEntity<?>> rawClass;
        final String database;
        final String table;
        /**
         * 代理增强类,
         * 只做对原始类进行包装工作，所以不能对代理类进行字段修改
         */
        final Class<? extends AbstractEntity<?>> proxyClass;
        final Constructor<? extends AbstractEntity<?>> constructor;
        final Field idField;
        /**
         * 数据库字段
         */
        final List<Field> properties;

        EntityInfo(Class<? extends AbstractEntity<?>> clazz) {
            var annotation = clazz.getAnnotation(Entity.class);
            if (annotation == null) {
                throw new IllegalStateException("没有定义@Entity注解");
            }
            this.rawClass = clazz;
            this.database = "".equals(annotation.database()) ? null : annotation.database();
            this.table = "".equals(annotation.database()) ? clazz.getSimpleName() : annotation.database();
            this.proxyClass = buildProxy(clazz);
            try {
                this.constructor = this.proxyClass.getDeclaredConstructor(clazz);
            } catch (NoSuchMethodException ex) {
                throw new UndeclaredThrowableException(ex);
            }
            var fields = new ArrayList<Field>();
            Field id = null;
            for (Field declaredField : clazz.getDeclaredFields()) {
                var modifiers = declaredField.getModifiers();
                if (!Modifier.isFinal(modifiers) && !Modifier.isTransient(modifiers)) {
                    declaredField.setAccessible(true);
                    if (declaredField.isAnnotationPresent(Id.class)) {
                        if (id == null) {
                            id = declaredField;
                        } else {
                            throw new IllegalStateException("重复主键");
                        }
                    } else {
                        fields.add(declaredField);
                    }
                }
            }
            if (id == null) {
                throw new IllegalStateException("没有定义主键@Id");
            }
            this.idField = id;
            this.properties = Collections.unmodifiableList(fields);
        }

        @SuppressWarnings("unchecked")
        <T extends AbstractEntity<?>> Class<T> buildProxy(Class<T> clazz) {
            try {
                return (Class<T>) Class.forName(clazz.getName() + "$Proxy");
            } catch (Exception ex) {
                throw new IllegalStateException("buildProxy error", ex);
            }
        }


        AbstractEntity<?> newObject(AbstractEntity<?> old, Object id) {
            try {
                var nw = constructor.newInstance(old);
                // 如果没有ID, 则创建ID
                if (old.id() == null) {
                    if (id == null) {
                        throw new IllegalStateException("没有设置ID");
                    }
                    idField.set(old, id);
                }
                return nw;
            } catch (Exception ex) {
                throw new IllegalStateException("创建增强对象失败", ex);
            }
        }
    }

}

