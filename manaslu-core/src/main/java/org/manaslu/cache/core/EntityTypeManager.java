package org.manaslu.cache.core;

import org.manaslu.cache.core.annotations.Entity;
import org.manaslu.cache.core.annotations.Id;
import org.manaslu.cache.core.annotations.SubEntity;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.UndeclaredThrowableException;
import java.util.*;
import java.util.stream.Collectors;

public class EntityTypeManager {

    private final Map<Class<?>, EntityInfo> registerTypes = new HashMap<>();
    private final Map<Class<?>, SubEntityInfo> registerSubTypes = new HashMap<>();

    public void registerTypes(List<Class<? extends AbstractEntity<?>>> registerClasses) {
        var map = new HashMap<Class<?>, EntityInfo>();
        for (Class<? extends AbstractEntity<?>> clazz : registerClasses) {
            if (map.containsKey(clazz)) {
                throw new IllegalStateException("出现重复类型" + clazz.getName());
            }
            map.put(clazz, new EntityInfo(this, clazz));
        }
        this.registerTypes.putAll(map);
    }

    Class<?> registerSubType(Class<?> clazz) {
        if (registerSubTypes.containsKey(clazz)) {
            return registerSubTypes.get(clazz).proxyClass;
        }
        var subEntityInfo = new SubEntityInfo(this, clazz);
        registerSubTypes.put(clazz, subEntityInfo);
        return subEntityInfo.proxyClass;
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
        return (E) entityInfo.newObject(this, entity, id);
    }

    @SuppressWarnings("unchecked")
    <ID extends Comparable<ID>, E extends AbstractEntity<ID>> Class<E> getProxyClass(Class<E> rawClass) {
        if (!registerTypes.containsKey(rawClass)) {
            throw new IllegalArgumentException("出现为注册类型" + rawClass.getName());
        }
        var entityInfo = registerTypes.get(rawClass);
        return (Class<E>) entityInfo.proxyClass;
    }

    /**
     * 获取类信息
     */
    public EntityTypeInfo getInfo(Class<? extends AbstractEntity<?>> type) {
        if (!registerTypes.containsKey(type)) {
            throw new IllegalArgumentException("出现为注册类型" + type.getName());
        }
        var entityInfo = registerTypes.get(type);
        Map<Class<?>, EntityTypeInfo.SubEntityTypeInfo> subs = registerSubTypes.values().stream()
                .collect(Collectors.toUnmodifiableMap(e -> e.rawClass, e -> new EntityTypeInfo.SubEntityTypeInfo(e.rawClass,
                        e.properties.stream().collect(Collectors.toUnmodifiableMap(ManasluField::getName, f -> f))))
                );
        return new EntityTypeInfo(entityInfo.rawClass, entityInfo.database, entityInfo.table,
                entityInfo.idField,
                entityInfo.properties.stream().collect(Collectors.toUnmodifiableMap(ManasluField::getName, e -> e)), subs);
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
        final List<ManasluField> properties;

        final List<ManasluField> enhancedProperties;

        EntityInfo(EntityTypeManager manager, Class<? extends AbstractEntity<?>> clazz) {
            this.proxyClass = buildProxy(clazz);
            var annotation = proxyClass.getAnnotation(Entity.class);
            if (annotation == null) {
                throw new IllegalStateException("没有定义@Entity注解");
            }
            this.rawClass = clazz;
            this.database = "".equals(annotation.database()) ? null : annotation.database();
            this.table = "".equals(annotation.database()) ? clazz.getSimpleName() : annotation.database();
            try {
                this.constructor = this.proxyClass.getDeclaredConstructor(clazz);
            } catch (NoSuchMethodException ex) {
                throw new UndeclaredThrowableException(ex);
            }
            var fields = new ArrayList<ManasluField>();
            var enhancedFields = new ArrayList<ManasluField>();
            Field id = null;
            for (Field declaredField : clazz.getDeclaredFields()) {
                var modifiers = declaredField.getModifiers();
                if (!Modifier.isFinal(modifiers) && !Modifier.isTransient(modifiers) && !Modifier.isStatic(modifiers)) {
                    if (!Modifier.isPrivate(modifiers)) {
                        throw new IllegalStateException("禁止使用非private的字段" + clazz.getName());
                    }
                    declaredField.setAccessible(true);
                    if (declaredField.isAnnotationPresent(Id.class)) {
                        if (id == null) {
                            id = declaredField;
                        } else {
                            throw new IllegalStateException("重复主键");
                        }
                    } else {
                        if (declaredField.getType().isAssignableFrom(SubEntity.class)) {
                            Class<?> proxy = manager.registerSubType(declaredField.getType());
                            var proxyField = new ProxyField(declaredField, proxy);
                            enhancedFields.add(proxyField);
                            fields.add(proxyField);
                        } else {
                            fields.add(new NormalField(declaredField));
                        }
                    }
                }
            }
            if (id == null) {
                throw new IllegalStateException("没有定义主键@Id");
            }
            this.idField = id;
            this.properties = Collections.unmodifiableList(fields);
            this.enhancedProperties = Collections.unmodifiableList(enhancedFields);
        }

        @SuppressWarnings("unchecked")
        <T extends AbstractEntity<?>> Class<T> buildProxy(Class<T> clazz) {
            try {
                return (Class<T>) Class.forName(clazz.getName() + "$Proxy");
            } catch (Exception ex) {
                throw new IllegalStateException("buildProxy error", ex);
            }
        }


        AbstractEntity<?> newObject(EntityTypeManager manager, AbstractEntity<?> old, Object id) {
            try {
                var nw = constructor.newInstance(old);
                // 如果没有ID, 则创建ID
                if (old.id() == null) {
                    if (id == null) {
                        throw new IllegalStateException("没有设置ID");
                    }
                    idField.set(old, id);
                }
                if (!enhancedProperties.isEmpty()) {
                    for (var enhancedProperty : enhancedProperties) {
                        var subEntityInfo = manager.registerSubTypes.get(enhancedProperty.getType());
                        enhancedProperty.set(nw, subEntityInfo.newObject(manager, old, enhancedProperty.get(old),
                                enhancedProperty.getName()));
                    }
                }
                return nw;
            } catch (Exception ex) {
                throw new IllegalStateException("创建增强对象失败", ex);
            }
        }
    }

    static class SubEntityInfo {
        final Class<?> rawClass;
        /**
         * 代理增强类,
         * 只做对原始类进行包装工作，所以不能对代理类进行字段修改
         */
        final Class<?> proxyClass;
        final Constructor<?> constructor;
        /**
         * 内部增强字段
         */
        final List<ManasluField> enhancedProperties;
        final List<ManasluField> properties;

        SubEntityInfo(EntityTypeManager manager, Class<?> clazz) {
            this.proxyClass = buildProxy(clazz);
            var annotation = proxyClass.getAnnotation(SubEntity.class);
            if (annotation == null) {
                throw new IllegalStateException("没有定义@SubEntity注解");
            }
            this.rawClass = clazz;
            try {
                this.constructor = this.proxyClass.getDeclaredConstructor(AbstractEntity.class, clazz, String.class);
            } catch (NoSuchMethodException ex) {
                throw new UndeclaredThrowableException(ex);
            }
            var fields = new ArrayList<ManasluField>();
            var enhancedFields = new ArrayList<ManasluField>();
            for (Field declaredField : clazz.getDeclaredFields()) {
                var modifiers = declaredField.getModifiers();
                if (!Modifier.isFinal(modifiers) && !Modifier.isTransient(modifiers) && !Modifier.isStatic(modifiers)) {
                    if (!Modifier.isPrivate(modifiers)) {
                        throw new IllegalStateException("禁止使用非private的字段" + clazz.getName());
                    }
                    declaredField.setAccessible(true);
                    if (declaredField.getType().isAssignableFrom(SubEntity.class)) {
                        Class<?> proxy = manager.registerSubType(declaredField.getType());
                        var proxyField = new ProxyField(declaredField, proxy);
                        enhancedFields.add(proxyField);
                        fields.add(proxyField);
                    } else {
                        fields.add(new NormalField(declaredField));
                    }
                }
            }

            this.enhancedProperties = Collections.unmodifiableList(enhancedFields);
            this.properties = Collections.unmodifiableList(fields);
        }

        @SuppressWarnings("unchecked")
        <T> Class<T> buildProxy(Class<T> clazz) {
            try {
                return (Class<T>) Class.forName(clazz.getName() + "$Proxy");
            } catch (Exception ex) {
                throw new IllegalStateException("buildProxy error", ex);
            }
        }


        Object newObject(EntityTypeManager manager, AbstractEntity<?> parent, Object old, String fieldName) {
            try {
                var nw = constructor.newInstance(parent, old, fieldName);
                if (!enhancedProperties.isEmpty()) {
                    for (var enhancedProperty : enhancedProperties) {
                        var subEntityInfo = manager.registerSubTypes.get(enhancedProperty.getType());
                        enhancedProperty.set(nw, subEntityInfo.newObject(manager, parent, enhancedProperty.get(old),
                                enhancedProperty.getName()));
                    }
                }
                return nw;
            } catch (Exception ex) {
                throw new IllegalStateException("创建增强对象失败", ex);
            }
        }
    }

}

