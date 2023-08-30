package org.manaslu.cache.core;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.manaslu.cache.core.annotations.Enhance;
import org.manaslu.cache.core.annotations.EnhanceEntity;
import org.manaslu.cache.core.annotations.Entity;
import org.manaslu.cache.core.annotations.Id;

import javax.annotation.Nonnull;

@EqualsAndHashCode(callSuper = true)
@Entity(database = "aaa", table = "bbb")
@EnhanceEntity
@Data
public class UserEntity extends AbstractEntity<Integer> {

    @Id
    private int id;

    private String name;

    @Override
    public Integer id() {
        return id;
    }

    @Enhance({"name"})
    public void update(String name) {
        this.name = name;
    }

    void hello() {

    }

    int add(int a, int b, @Nonnull Object obj) {
        return a + b;
    }
}
