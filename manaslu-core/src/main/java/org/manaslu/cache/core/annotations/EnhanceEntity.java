package org.manaslu.cache.core.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 增强方法
 */
@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.TYPE)
public @interface EnhanceEntity {

    /**
     * 表名
     */
    String table() default "";

    /**
     * 对应数据库
     */
    String database() default "";

    /**
     * 更新类型
     */
    Entity.UpdateType updateType() default Entity.UpdateType.PART;

    /**
     * 缓存策略
     */
    Entity.CacheStrategy cacheStrategy() default Entity.CacheStrategy.NO;

    /**
     * 选择LRU配置时，最大数量
     */
    int lruMaxSize() default Integer.MAX_VALUE;

    /**
     * 选择LRU配置时，超时时间
     */
    long lruMaxExpireTime() default -1;

    /**
     * 更新策略
     */
    Entity.DumpStrategy dumpStrategy() default Entity.DumpStrategy.IMMEDIATE;

    /**
     * 定时入库的间隔
     */
    long intervalScheduleTime() default 60000L;

    /**
     * 多少数量触发入库
     */
    int maxCountTriggerDump() default Integer.MAX_VALUE;
}
