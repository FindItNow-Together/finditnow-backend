package com.finditnow.database;

import java.sql.Connection;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.finditnow.config.Config;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

public class Database {
    private static final Logger logger = LoggerFactory.getLogger(Database.class);
    private HikariDataSource dataSource;

    public Database() {
        HikariConfig dbConfig = new HikariConfig();

        dbConfig.setJdbcUrl(
                Config.get("JDBC_DATABASE_URL",
                        "jdbc:mysql://localhost:3306/finditnow?useSSL=false&serverTimezone=UTC"));

        dbConfig.setUsername(Config.get("DATABASE_USER", "root"));
        dbConfig.setPassword(Config.get("DATABASE_USER_PWD", "root@123"));
        dbConfig.setMaximumPoolSize(Integer.parseInt(Config.get("DATABASE_POOL_SIZE", "5")));

        dataSource = new HikariDataSource(dbConfig);

        try(Connection conn = dataSource.getConnection()){
            logger.info("Database connection successful");
        }catch(Exception e){
            logger.error("Failed to connect to database", e);
            throw new RuntimeException("Failed to connect to Database", e);
        }
    }

    public DataSource get() {
        return dataSource;
    }
}

