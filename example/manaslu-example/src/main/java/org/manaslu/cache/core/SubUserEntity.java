package org.manaslu.cache.core;

import lombok.Data;
import org.manaslu.cache.core.annotations.Enhance;
import org.manaslu.cache.core.annotations.SubEnhanceEntity;

import javax.annotation.Nonnull;

@SubEnhanceEntity
@Data
public class SubUserEntity {

    private int id;

    private String name;

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
