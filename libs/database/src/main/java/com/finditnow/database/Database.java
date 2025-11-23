package com.finditnow.database;

import com.finditnow.config.Config;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.sql.Connection;
import java.util.TimeZone;

public class Database {
    private static final Logger logger = LoggerFactory.getLogger(Database.class);
    private HikariDataSource dataSource;

    public Database() {


        HikariConfig dbConfig = new HikariConfig();


        dbConfig.setJdbcUrl(
                Config.get("JDBC_DATABASE_URL",
                        "DB_URL"));

        dbConfig.setUsername(Config.get("DATABASE_USER", "root"));
        dbConfig.setPassword(Config.get("DATABASE_USER_PWD", "root@123"));
        dbConfig.setMaximumPoolSize(Integer.parseInt(Config.get("DATABASE_POOL_SIZE", "5")));
        TimeZone.setDefault(TimeZone.getTimeZone(Config.get("DATABASE_TIMEZONE", "UTC")));

        dataSource = new HikariDataSource(dbConfig);

        try (Connection conn = dataSource.getConnection()) {
            logger.info("Database connection successful");
        } catch (Exception e) {
            logger.error("Failed to connect to database", e);
            throw new RuntimeException("Failed to connect to Database", e);
        }
    }

    public DataSource get() {
        return dataSource;
    }
}

