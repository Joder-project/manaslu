package org.manaslu.cache.spring;

import org.manaslu.cache.core.DbOperatorFactory;
import org.manaslu.cache.core.EntityTypeManager;
import org.manaslu.cache.mysql.MysqlConnections;
import org.manaslu.cache.mysql.MysqlDbOperator;
import org.manaslu.cache.mysql.MysqlDbOperatorFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@ConditionalOnClass({MysqlDbOperator.class, MysqlDbOperatorFactory.class})
@EnableConfigurationProperties({ManasluProperties.class, MysqlManasluProperties.class})
@AutoConfiguration
public class MysqlManasluConfiguration {

    @Bean
    DbOperatorFactory dbOperatorFactory(MysqlConnections connections, EntityTypeManager entityTypeManager) {
        return new MysqlDbOperatorFactory(connections, entityTypeManager);
    }

    @Bean
    MysqlConnections mysqlConnections(MysqlManasluProperties properties) {
        return new MysqlConnections(properties.getConfig());
    }
}
