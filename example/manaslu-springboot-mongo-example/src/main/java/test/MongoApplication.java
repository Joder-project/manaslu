package test;

import org.manaslu.cache.spring.EnableManaslu;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@EnableManaslu
public class MongoApplication {


    public static void main(String[] args) {
        SpringApplication.run(MongoApplication.class, args);
    }

}
