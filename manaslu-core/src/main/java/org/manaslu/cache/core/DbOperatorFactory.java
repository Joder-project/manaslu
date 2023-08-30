package org.manaslu.cache.core;

public interface DbOperatorFactory {

    /**
     * 获取数据库操作
     */
    <ID extends Comparable<ID>, E extends AbstractEntity<ID>> DbOperator<ID, E> create(Class<E> e);
}
