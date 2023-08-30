package org.manaslu.cache.core;

public abstract class AbstractEntity<ID extends Comparable<ID>> {

    CacheStrategy<ID, ? extends AbstractEntity<ID>> cacheStrategy;
    DumpStrategy<ID, ? extends AbstractEntity<ID>> dumpStrategy;

    /**
     * 主键
     */
    public abstract ID id();

    void initialize(CacheStrategy<ID, ? extends AbstractEntity<ID>> cacheStrategy, DumpStrategy<ID, ? extends AbstractEntity<ID>> dumpStrategy) {
        this.cacheStrategy = cacheStrategy;
        this.dumpStrategy = dumpStrategy;
    }
}
