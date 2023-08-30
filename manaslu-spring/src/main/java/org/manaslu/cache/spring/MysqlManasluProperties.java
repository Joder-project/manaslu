package org.manaslu.cache.spring;

import com.zaxxer.hikari.HikariConfig;
import lombok.Data;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@Data
@ConfigurationProperties(MysqlManasluProperties.PATH)
@ConditionalOnClass(HikariConfig.class)
public class MysqlManasluProperties {
    static final String PATH = ManasluProperties.PATH + ".mysql";
    private String driverClassName;
    private String jdbcUrl;
    private String username;
    private String password;

    private HikariConfig hikari = new HikariConfig();

    public HikariConfig getConfig() {
        hikari.setDriverClassName(Optional.ofNullable(hikari.getDriverClassName()).orElse(this.driverClassName));
        hikari.setJdbcUrl(Optional.ofNullable(hikari.getJdbcUrl()).orElse(this.jdbcUrl));
        hikari.setUsername(Optional.ofNullable(hikari.getUsername()).orElse(this.username));
        hikari.setPassword(Optional.ofNullable(hikari.getPassword()).orElse(this.password));
        return hikari;
    }
}
