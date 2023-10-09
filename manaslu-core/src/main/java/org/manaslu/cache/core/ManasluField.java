package org.manaslu.cache.core;

import org.manaslu.cache.core.exception.ManasluException;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.lang.reflect.Field;

/**
 * 字段包装
 */
public interface ManasluField {
    Class<?> getType();

    String getName();

    Object get(Object target) throws Exception;

    void set(Object target, Object value) throws Exception;
}

class NormalField implements ManasluField {
    private final VarHandle rawField;
    private final String name;

    NormalField(MethodHandles.Lookup lookup, Field rawField) throws Throwable {
        this.rawField = lookup.unreflectVarHandle(rawField);
        this.name = rawField.getName();
    }

    @Override
    public Class<?> getType() {
        return rawField.varType();
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Object get(Object target) throws Exception {
        return rawField.get(target);
    }

    @Override
    public void set(Object target, Object value) throws Exception {
        rawField.set(target, value);
    }
}

class ProxyField implements ManasluField {
    private final VarHandle rawField;
    private final String name;
    private final VarHandle rawObjectField;


    ProxyField(MethodHandles.Lookup lookup, Field rawField, Class<?> rawClass, Class<?> parentProxyClass) throws Exception {
        this.rawField = lookup.unreflectVarHandle(rawField);
        this.name = rawField.getName();
        try {
            this.rawObjectField = lookup.findVarHandle(parentProxyClass, "_raw", rawClass);
        } catch (Exception ex) {
            throw new ManasluException("找不到字段_raw");
        }
    }

    @Override
    public Class<?> getType() {
        return rawField.varType();
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Object get(Object target) throws Exception {
        Object object = target;
        if (target.getClass().getName().endsWith("$Proxy")) {
            object = rawObjectField.get(target);
        }
        if (object == null) {
            return null;
        }
        return rawField.get(object);
    }

    @Override
    public void set(Object target, Object value) throws Exception {
        Object object = target;
        if (target.getClass().getName().endsWith("$Proxy")) {
            object = rawObjectField.get(target);
        }
        if (object == null) {
            return;
        }
        rawField.set(object, value);
    }
}