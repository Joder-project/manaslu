package org.manaslu.cache.spring;

import org.manaslu.cache.core.DbOperatorFactory;
import org.manaslu.cache.core.EntityTypeManager;
import org.manaslu.cache.core.RepositoryFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties({ManasluProperties.class})
@AutoConfiguration
public class ManasluConfiguration {

    @Bean
    @ConditionalOnMissingBean
    EntityTypeManager entityTypeManager() {
        return new EntityTypeManager();
    }

    @Bean
    RepositoryFactory repositoryFactory(EntityTypeManager entityTypeManager, DbOperatorFactory dbOperatorFactory,
                                        @Qualifier("manasluScheduler") ScheduledExecutorService scheduledExecutorService) {
        return new RepositoryFactory(entityTypeManager, dbOperatorFactory, scheduledExecutorService);
    }

    @Bean("manasluScheduler")
    ScheduledExecutorService scheduledThreadPoolExecutor(ManasluProperties properties) {
        return Executors.newScheduledThreadPool(properties.getScheduleNum());
    }
}
