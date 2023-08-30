package test;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bson.types.ObjectId;
import org.manaslu.cache.core.Repository;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
@Slf4j
public class UserService {

    private final Repository<ObjectId, User> userRepository;

    @PostConstruct
    void init() {
        var user = new User();
        user = userRepository.create(user);
        user.update("Hello");
        var time = System.nanoTime();
        for (int i = 0; i < 1000; i++) {
            assert "Hello".equals(userRepository.load(user.id()).map(User::getName).orElse(null));
        }
        log.info("time: {} ns", System.nanoTime() - time);
        log.info("user id: {}", user.id());
    }

    @PreDestroy
    void destroy() {
        userRepository.listFromCache().forEach(user -> userRepository.delete(user.id()));
    }
}
