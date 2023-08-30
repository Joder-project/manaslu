package org.manaslu.cache.mysql;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.Connection;
import java.sql.SQLException;

public class MysqlConnections {

    private final HikariDataSource dataSource;

    public MysqlConnections(HikariConfig config) {
        this.dataSource = new HikariDataSource(config);
    }

    Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

}
