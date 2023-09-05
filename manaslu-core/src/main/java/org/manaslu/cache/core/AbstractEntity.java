package org.manaslu.cache.core;

public abstract class AbstractEntity<ID extends Comparable<ID>> {

    protected CacheStrategy<?, ? extends AbstractEntity<?>> cacheStrategy;
    protected DumpStrategy<?, ? extends AbstractEntity<?>> dumpStrategy;

    /**
     * 主键
     */
    public abstract ID id();

    public void initialize(CacheStrategy<?, ? extends AbstractEntity<?>> cacheStrategy, DumpStrategy<?, ? extends AbstractEntity<?>> dumpStrategy) {
        this.cacheStrategy = cacheStrategy;
        this.dumpStrategy = dumpStrategy;
    }

    public DumpStrategy<?, ? extends AbstractEntity<?>> dumpStrategy() {
        return dumpStrategy;
    }

    public AbstractEntity<?> entity() {
        return this;
    }

    /**
     * 构造完成后
     */
    public void postLoad() {

    }
}
