package org.manaslu.cache.core;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.manaslu.cache.core.annotations.Enhance;
import org.manaslu.cache.core.annotations.Entity;

@EqualsAndHashCode(callSuper = true)
@Entity
@Data
public class UserEntity extends AbstractEntity<Integer> {

    private int id;

    private String name;

    @Override
    public Integer id() {
        return id;
    }

    @Enhance("name")
    public boolean update() {
        return true;
    }
}
