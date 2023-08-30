package org.manaslu.cache.core;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.ClassFileVersion;
import net.bytebuddy.NamingStrategy;
import net.bytebuddy.description.modifier.Visibility;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;
import net.bytebuddy.implementation.MethodDelegation;
import net.bytebuddy.implementation.SuperMethodCall;
import net.bytebuddy.matcher.ElementMatchers;
import org.manaslu.cache.core.annotations.Entity;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.UndeclaredThrowableException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EntityTypeManager {

    private final Map<Class<?>, EntityInfo> registerTypes;

    public EntityTypeManager(List<Class<? extends AbstractEntity<?>>> registerType) {
        var map = new HashMap<Class<?>, EntityInfo>();
        for (Class<? extends AbstractEntity<?>> clazz : registerType) {
            if (map.containsKey(clazz)) {
                throw new IllegalStateException("出现重复类型" + clazz.getName());
            }
            map.put(clazz, new EntityInfo(clazz));
        }
        this.registerTypes = map;
    }

    @SuppressWarnings("unchecked")
    <ID extends Comparable<ID>, E extends AbstractEntity<ID>> E newEnhance(E entity) {
        if (!registerTypes.containsKey(entity.getClass())) {
            throw new IllegalArgumentException("出现为注册类型" + entity.getClass().getName());
        }
        var entityInfo = registerTypes.get(entity.getClass());
        return (E) entityInfo.newObject(entity);
    }
}

/**
 * 类型对应信息
 */
class EntityInfo {

    final static String RawName = EnhancedMethodIntercept.DEFAULT_INJECT_FIELD;

    final Class<? extends AbstractEntity<?>> rawClass;
    final String database;
    final String table;
    /**
     * 代理增强类
     */
    final Class<? extends AbstractEntity<?>> proxyClass;
    final Constructor<? extends AbstractEntity<?>> constructor;
    final Field rawObjField;

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
            this.constructor = this.proxyClass.getDeclaredConstructor();
            this.rawObjField = this.proxyClass.getDeclaredField(RawName);
        } catch (NoSuchMethodException | NoSuchFieldException ex) {
            throw new UndeclaredThrowableException(ex);
        }
    }

    @SuppressWarnings("unchecked")
    <T extends AbstractEntity<?>> Class<T> buildProxy(Class<T> clazz) {
        DynamicType.Builder<T> builder = new ByteBuddy(ClassFileVersion.JAVA_V11)
                .with(new NamingStrategy.AbstractBase() {
                    @Nonnull
                    @Override
                    protected String name(@Nonnull TypeDescription superClass) {
                        return superClass.getName() + "_entityProxy";
                    }
                })
                .subclass(clazz)
                .annotateType(clazz.getAnnotations());
        builder = definitionMethod(builder, clazz);
        builder = definitionConstructor(builder, clazz);
        // 定义一个原先对象
        builder = builder.defineField(RawName, clazz, Visibility.PUBLIC);
        try (DynamicType.Loaded<T> dynamicType = builder.make()
                .load(clazz.getClassLoader(), ClassLoadingStrategy.Default.INJECTION)) {
            return (Class<T>) dynamicType.getLoaded();
        } catch (IOException ex) {
            throw new IllegalStateException("buildProxy error", ex);
        }
    }

    /**
     * 补构造方法注解
     */
    private <T extends AbstractEntity<?>> DynamicType.Builder<T> definitionConstructor(DynamicType.Builder<T> builder, Class<T> rawClazz) {
        for (Constructor<?> constructor : rawClazz.getDeclaredConstructors()) {
            DynamicType.Builder.MethodDefinition<T> definition = builder
                    .constructor(ElementMatchers.takesArguments(constructor.getParameterTypes()))
                    .intercept(SuperMethodCall.INSTANCE)
                    .annotateMethod(constructor.getDeclaredAnnotations());
            for (int i = 0; i < constructor.getParameterAnnotations().length; i++) {
                definition = definition.annotateParameter(i, constructor.getParameterAnnotations()[i]);
            }
            builder = definition;

        }
        return builder;
    }

    /**
     * 补方法注解
     */
    private <T extends AbstractEntity<?>> DynamicType.Builder<T> definitionMethod(DynamicType.Builder<T> builder,
                                                                                  Class<T> rawClazz) {
        for (Method method : rawClazz.getDeclaredMethods()) {
            DynamicType.Builder.MethodDefinition.ImplementationDefinition<T> definition = builder.method(ElementMatchers.is(method));
            DynamicType.Builder.MethodDefinition.ReceiverTypeDefinition<T> intercept;
            intercept = definition.intercept(MethodDelegation.to(EnhancedMethodIntercept.class));
            DynamicType.Builder.MethodDefinition<T> tMethodDefinition = intercept.annotateMethod(method.getDeclaredAnnotations());
            for (int i = 0; i < method.getParameterAnnotations().length; i++) {
                tMethodDefinition = tMethodDefinition.annotateParameter(i, method.getParameterAnnotations()[i]);
            }
            builder = tMethodDefinition;
        }
        return builder;
    }

    AbstractEntity<?> newObject(AbstractEntity<?> old) {
        try {
            AbstractEntity<?> abstractEntity = constructor.newInstance();
            rawObjField.set(abstractEntity, old);
            return abstractEntity;
        } catch (Exception ex) {
            throw new IllegalStateException("创建增强对象失败", ex);
        }
    }
}