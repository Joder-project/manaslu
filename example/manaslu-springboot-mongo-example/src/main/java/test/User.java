package test;

import lombok.Getter;
import org.bson.types.ObjectId;
import org.manaslu.cache.core.AbstractEntity;
import org.manaslu.cache.core.annotations.*;
import org.manaslu.cache.spring.GenerateRepository;

@GenerateRepository
@EnhanceEntity(cacheStrategy = Entity.CacheStrategy.PERSIST, dumpStrategy = Entity.DumpStrategy.INTERVAL, intervalScheduleTime = 5000L)
public class User extends AbstractEntity<ObjectId> {

    @Id
    private ObjectId id;
    @Getter
    private String name;
    @Getter
    private SubUser subUser = new SubUser();

    @Override
    public ObjectId id() {
        return id;
    }

    @Enhance("name")
    void update(String name) {
        this.name = name;
    }

}

@SubEntity
@SubEnhanceEntity
class SubUser {
    private String email;
    @Getter
    private SubSubUser subUser = new SubSubUser();

    public String getEmail() {
        return email;
    }

    @Enhance
    void change(String email) {
        this.email = email;
    }
}

@SubEntity
@SubEnhanceEntity
class SubSubUser {
    private String email;

    public String getEmail() {
        return email;
    }

    @Enhance
    void change(String email) {
        this.email = email;
    }
}
