package org.manaslu.cache.spring;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import org.manaslu.cache.core.DbOperatorFactory;
import org.manaslu.cache.core.EntityTypeManager;
import org.manaslu.cache.mongo.MongoDbOperator;
import org.manaslu.cache.mongo.MongoDbOperatorFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@ConditionalOnClass({MongoDbOperatorFactory.class, MongoDbOperator.class})
@EnableConfigurationProperties({ManasluProperties.class})
@AutoConfiguration
public class MongoManasluConfiguration {

    @Bean
    DbOperatorFactory dbOperatorFactory(ManasluProperties properties, MongoClient mongoClient, EntityTypeManager entityTypeManager) {
        return new MongoDbOperatorFactory(properties.getMongo().getDatabase(), mongoClient, entityTypeManager);
    }


    @Bean
    MongoClient mongoClient(ManasluProperties properties) {
        return MongoClients.create(properties.getMongo().getUrl());
    }
}
