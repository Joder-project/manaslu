package org.manaslu.cache.spring;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@Data
@ConfigurationProperties(ManasluProperties.PATH)
public class ManasluProperties {
    static final String PATH = "manaslu";
    static final String BASE_PACKAGES = PATH + ".base-packages";

    /**
     * 定时线程数
     */
    private int scheduleNum;

    /**
     * mongo 配置
     */
    private MongoProperties mongo = new MongoProperties();

    @Data
    public static class MongoProperties {
        private String url;
        private String database;
    }
}
