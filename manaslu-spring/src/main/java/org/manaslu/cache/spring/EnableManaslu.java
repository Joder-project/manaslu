package org.manaslu.cache.spring;

import org.springframework.context.annotation.Import;

import java.lang.annotation.*;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Import({MongoManasluConfiguration.class, ManasluConfiguration.class, MysqlManasluConfiguration.class})
public @interface EnableManaslu {
}
