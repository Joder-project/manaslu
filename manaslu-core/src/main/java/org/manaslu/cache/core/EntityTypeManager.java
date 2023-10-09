package org.manaslu.cache.core;

import lombok.extern.slf4j.Slf4j;
import org.manaslu.cache.core.annotations.Entity;
import org.manaslu.cache.core.annotations.Id;
import org.manaslu.cache.core.annotations.SubEntity;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.UndeclaredThrowableException;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
public class EntityTypeManager {

    static final MethodHandles.Lookup LOOKUP = MethodHandles.lookup();

    private final Map<Class<?>, EntityInfo> registerTypes = new HashMap<>();
    private final Map<Class<?>, SubEntityInfo> registerSubTypes = new HashMap<>();

    public void registerTypes(List<Class<? extends AbstractEntity<?>>> registerClasses) {
        var map = new HashMap<Class<?>, EntityInfo>();
        for (Class<? extends AbstractEntity<?>> clazz : registerClasses) {
            if (map.containsKey(clazz)) {
                throw new IllegalStateException("出现重复类型" + clazz.getName());
            }
            try {
                map.put(clazz, new EntityInfo(this, clazz));
            } catch (Throwable ex) {
                log.error("注册实体异常", ex);
            }
        }
        this.registerTypes.putAll(map);
    }

    void registerSubType(Class<?> clazz) {
        if (registerSubTypes.containsKey(clazz)) {
            return;
        }
        try {
            var subEntityInfo = new SubEntityInfo(this, clazz);
            registerSubTypes.put(clazz, subEntityInfo);
        } catch (Throwable ex) {
            log.error("注册实体异常", ex);
        }
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
        Map<Class<?>, EntityTypeInfo.SubEntityTypeInfo> subs = registerSubTypes.values().stream().collect(Collectors.toUnmodifiableMap(e -> e.rawClass, e -> new EntityTypeInfo.SubEntityTypeInfo(e.rawClass, e.properties.stream().collect(Collectors.toUnmodifiableMap(ManasluField::getName, f -> f)))));
        return new EntityTypeInfo(entityInfo.rawClass, entityInfo.database, entityInfo.table, entityInfo.idField, entityInfo.properties.stream().collect(Collectors.toUnmodifiableMap(ManasluField::getName, e -> e)), subs);
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
        final MethodHandle constructor;
        final ManasluField idField;
        /**
         * 数据库字段
         */
        final List<ManasluField> properties;

        final List<ManasluField> enhancedProperties;
        final MethodHandles.Lookup selfLookup;

        EntityInfo(EntityTypeManager manager, Class<? extends AbstractEntity<?>> clazz) throws Throwable {
            this.selfLookup = MethodHandles.privateLookupIn(clazz, LOOKUP);
            this.proxyClass = buildProxy(clazz);
            var annotation = proxyClass.getAnnotation(Entity.class);
            if (annotation == null) {
                throw new IllegalStateException("没有定义@Entity注解");
            }
            this.rawClass = clazz;
            this.database = "".equals(annotation.database()) ? null : annotation.database();
            this.table = "".equals(annotation.database()) ? clazz.getSimpleName() : annotation.database();
            try {
                this.constructor = LOOKUP.findConstructor(this.proxyClass, MethodType.methodType(void.class, clazz));
            } catch (IllegalAccessException | NoSuchMethodException ex) {
                throw new UndeclaredThrowableException(ex);
            }
            var fields = new ArrayList<ManasluField>();
            var enhancedFields = new ArrayList<ManasluField>();
            ManasluField id = null;
            for (Field declaredField : clazz.getDeclaredFields()) {
                var modifiers = declaredField.getModifiers();
                if (!Modifier.isFinal(modifiers) && !Modifier.isTransient(modifiers) && !Modifier.isStatic(modifiers)) {
                    if (!Modifier.isPrivate(modifiers)) {
                        throw new IllegalStateException("禁止使用非private的字段" + clazz.getName());
                    }
                    declaredField.setAccessible(true);
                    if (declaredField.isAnnotationPresent(Id.class)) {
                        if (id == null) {
                            id = new NormalField(selfLookup, declaredField);
                        } else {
                            throw new IllegalStateException("重复主键");
                        }
                    } else {
                        var proxyField = new ProxyField(selfLookup, declaredField, LOOKUP.findVarHandle(proxyClass, "_raw", clazz));
                        if (declaredField.getType().isAnnotationPresent(SubEntity.class)) {
                            manager.registerSubType(declaredField.getType());

                            enhancedFields.add(proxyField);
                            fields.add(proxyField);
                        } else {
                            fields.add(proxyField);
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
            if (old == null) {
                throw new IllegalStateException("增强对象不能为空" + proxyClass.getName());
            }
            try {
                var nw = (AbstractEntity<?>) constructor.invoke(old);
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
                        enhancedProperty.set(old, subEntityInfo.newObject(manager, nw, enhancedProperty.get(old), enhancedProperty.getName()));
                    }
                }
                return nw;
            } catch (Throwable ex) {
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
        final MethodHandle constructor;
        /**
         * 内部增强字段
         */
        final List<ManasluField> enhancedProperties;
        final List<ManasluField> properties;
        final MethodHandles.Lookup selfLookup;

        SubEntityInfo(EntityTypeManager manager, Class<?> clazz) throws Throwable {
            this.selfLookup = MethodHandles.privateLookupIn(clazz, LOOKUP);
            this.proxyClass = buildProxy(clazz);
            var annotation = proxyClass.getAnnotation(SubEntity.class);
            if (annotation == null) {
                throw new IllegalStateException("没有定义@SubEntity注解");
            }
            this.rawClass = clazz;
            try {
                this.constructor = LOOKUP.findConstructor(this.proxyClass, MethodType.methodType(void.class, AbstractEntity.class, clazz, String.class));
            } catch (NoSuchMethodException | IllegalAccessException ex) {
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
                    var proxyField = new ProxyField(selfLookup, declaredField, LOOKUP.findVarHandle(proxyClass, "_raw", clazz));
                    if (declaredField.getType().isAnnotationPresent(SubEntity.class)) {
                        manager.registerSubType(declaredField.getType());
                        enhancedFields.add(proxyField);
                        fields.add(proxyField);
                    } else {
                        fields.add(proxyField);
                    }
                }
            }

            this.enhancedProperties = Collections.unmodifiableList(enhancedFields);
            this.properties = Collections.unmodifiableList(fields);
        }

        @SuppressWarnings("unchecked")
        <T> Class<T> buildProxy(Class<T> clazz) {
            try {

                return (Class<T>) LOOKUP.findClass(clazz.getName() + "$Proxy");
            } catch (Exception ex) {
                throw new IllegalStateException("buildProxy error", ex);
            }
        }


        Object newObject(EntityTypeManager manager, AbstractEntity<?> parent, Object old, String fieldName) {
            if (old == null) {
                throw new IllegalStateException("增强对象字段不能为空" + fieldName + ", " + proxyClass.getName());
            }
            try {
                var nw = constructor.invoke(parent, old, fieldName);
                if (!enhancedProperties.isEmpty()) {
                    for (var enhancedProperty : enhancedProperties) {
                        var subEntityInfo = manager.registerSubTypes.get(enhancedProperty.getType());
                        enhancedProperty.set(old, subEntityInfo.newObject(manager, parent, enhancedProperty.get(old), enhancedProperty.getName()));
                    }
                }
                return nw;
            } catch (Throwable ex) {
                throw new IllegalStateException("创建增强对象失败", ex);
            }
        }
    }

}

