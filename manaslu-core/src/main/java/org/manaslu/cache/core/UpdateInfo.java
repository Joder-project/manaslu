package org.manaslu.cache.core;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * 更新信息
 *
 * @param entity           数据
 * @param updateProperties 更新字段
 */
public record UpdateInfo<ID extends Comparable<ID>, Entity extends AbstractEntity<ID>>(Entity entity,
                                                                                       @Nonnull Set<String> updateProperties) {

    UpdateInfo<ID, Entity> merge(UpdateInfo<ID, Entity> other) {
        if (entity != other.entity()) {
            throw new IllegalStateException("不同值不能合并");
        }
        var hashSet = new HashSet<>(updateProperties);
        hashSet.addAll(other.updateProperties);
        return new UpdateInfo<>(entity, Collections.unmodifiableSet(hashSet));
    }
}
