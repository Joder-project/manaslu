package org.manaslu.cache.core;

public abstract class AbstractEntity<ID extends Comparable<ID>> {

    protected CacheStrategy<ID, ? extends AbstractEntity<ID>> cacheStrategy;
    protected DumpStrategy<ID, ? extends AbstractEntity<ID>> dumpStrategy;

    /**
     * 主键
     */
    public abstract ID id();

    void initialize(CacheStrategy<ID, ? extends AbstractEntity<ID>> cacheStrategy, DumpStrategy<ID, ? extends AbstractEntity<ID>> dumpStrategy) {
        this.cacheStrategy = cacheStrategy;
        this.dumpStrategy = dumpStrategy;
    }

    public DumpStrategy<ID, ? extends AbstractEntity<ID>> dumpStrategy() {
        return dumpStrategy;
    }

    /**
     * 构造完成后
     */
    public void postLoad() {

    }
}
