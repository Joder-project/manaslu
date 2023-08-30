package org.manaslu.cache.core;

public class RepositoryFactory {

    /**
     * 获取对应实体类的存储类
     *
     * @param entityType 实体类型
     * @param <ID>       主键
     * @param <E>        实体
     */
    public <ID extends Comparable<ID>, E extends AbstractEntity<ID>> Repository<ID, E> getRepository(Class<E> entityType) {
        return null;
    }
}
