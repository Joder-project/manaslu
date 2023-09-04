package org.manaslu.cache.core;

import org.manaslu.cache.core.exception.ManasluException;

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
    private final Field rawField;

    NormalField(Field rawField) {
        this.rawField = rawField;
    }

    @Override
    public Class<?> getType() {
        return rawField.getType();
    }

    @Override
    public String getName() {
        return rawField.getName();
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
    private final Field rawField;

    private final Field rawObjectField;

    ProxyField(Field rawField, Class<?> proxyClass) {
        this.rawField = rawField;
        try {
            this.rawObjectField = proxyClass.getDeclaredField("_raw");
        } catch (Exception ex) {
            throw new ManasluException("找不到字段_raw");
        }
    }

    @Override
    public Class<?> getType() {
        return rawField.getType();
    }

    @Override
    public String getName() {
        return rawField.getName();
    }

    @Override
    public Object get(Object target) throws Exception {
        var object = rawObjectField.get(target);
        if (object == null) {
            return null;
        }
        return rawField.get(object);
    }

    @Override
    public void set(Object target, Object value) throws Exception {
        var object = rawObjectField.get(target);
        if (object == null) {
            return;
        }
        rawField.set(object, value);
    }
}