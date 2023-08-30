package org.manaslu.cache.core.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 实体标注
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Entity {
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
    UpdateType updateType() default UpdateType.ALL;

    /**
     * 缓存策略
     */
    CacheStrategy cacheStrategy() default CacheStrategy.NO;

    /**
     * 更新策略
     */
    DumpStrategy dumpStrategy() default DumpStrategy.IMMEDIATE;

    enum UpdateType {
        /**
         * 全量更新
         */
        ALL,
        /**
         * 只更新@Enhance指定属性
         */
        PART
    }

    enum CacheStrategy {
        /**
         * lru淘汰策略
         */
        LRU,
        /**
         * 持久化淘汰策略
         */
        PERSIST,
        /**
         * 不缓存
         */
        NO
    }

    enum DumpStrategy {
        /**
         * 立即入库
         */
        IMMEDIATE,
        /**
         * 定时入库
         */
        INTERVAL,
        /**
         * 按更新数量入库
         */
        COUNTER,
        /**
         * 按数量和定时入库
         */
        COUNTER_INTERVAL
    }

}
