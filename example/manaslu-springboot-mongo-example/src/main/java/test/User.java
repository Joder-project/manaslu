package test;

import lombok.Getter;
import org.bson.types.ObjectId;
import org.manaslu.cache.core.AbstractEntity;
import org.manaslu.cache.core.annotations.Enhance;
import org.manaslu.cache.core.annotations.EnhanceEntity;
import org.manaslu.cache.core.annotations.Entity;
import org.manaslu.cache.core.annotations.Id;
import org.manaslu.cache.spring.GenerateRepository;

@GenerateRepository
@Entity(cacheStrategy = Entity.CacheStrategy.PERSIST, dumpStrategy = Entity.DumpStrategy.INTERVAL, intervalScheduleTime = 5000L)
@EnhanceEntity
public class User extends AbstractEntity<ObjectId> {

    @Id
    private ObjectId id;
    @Getter
    private String name;

    @Override
    public ObjectId id() {
        return id;
    }

    @Enhance("name")
    void update(String name) {
        this.name = name;
    }
}
