package org.manaslu.cache.core;

import net.bytebuddy.implementation.bind.annotation.AllArguments;
import net.bytebuddy.implementation.bind.annotation.Origin;
import net.bytebuddy.implementation.bind.annotation.RuntimeType;
import net.bytebuddy.implementation.bind.annotation.This;
import org.manaslu.cache.core.annotations.Enhance;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.stream.Collectors;

public class EnhancedMethodIntercept {

    public static final String DEFAULT_INJECT_FIELD = "_rawObj";

    @SuppressWarnings({"unchecked", "rawtypes"})
    @RuntimeType
    public static Object intercept(@This Object object, @Origin Method method, @AllArguments Object[] arguments) throws Exception {
        var declaredField = object.getClass().getDeclaredField(DEFAULT_INJECT_FIELD);
        var entity = (AbstractEntity<?>) declaredField.get(object);
        var target = (AbstractEntity<?>) object;
        var ret = method.invoke(entity, arguments);
        var annotation = method.getAnnotation(Enhance.class);
        if (annotation != null) {
            var changes = Arrays.stream(annotation.value()).collect(Collectors.toUnmodifiableSet());
            target.dumpStrategy.update(new UpdateInfo(entity, changes));
        }
        return ret;
    }


}
