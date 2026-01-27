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
    private final HikariDataSource dataSource;

    public Database(String serviceName) {
        if (serviceName == null || serviceName.isEmpty()) {
            throw new IllegalArgumentException("serviceName:dbName cannot be null or empty");
        }
        HikariConfig dbConfig = new HikariConfig();
        dbConfig.setJdbcUrl("jdbc:postgresql://" + Config.get("DB_HOST", "localhost") + ":"
                + Config.get("DB_PORT", "5432") + "/" + serviceName);

        dbConfig.setUsername(Config.get("DB_USER", "devuser"));
        dbConfig.setPassword(Config.get("DB_PASSWORD", "dev@123"));
        dbConfig.setMaximumPoolSize(Integer.parseInt(Config.get("DB_POOL_SIZE", "5")));
        TimeZone.setDefault(TimeZone.getTimeZone(Config.get("DB_TIMEZONE", "Asia/Kolkata")));

        dataSource = new HikariDataSource(dbConfig);

        try (Connection conn = dataSource.getConnection()) {
            logger.info("Database connection successful");
        } catch (Exception e) {
            logger.error("Failed to connect to database", e);
            throw new RuntimeException("Failed to connect to Database", e);
        }
    }

    public static void setEnv(String serviceName) {
        System.setProperty("SERVICE_PORT", Config.get(serviceName.toUpperCase() + "_PORT", "8081"));
        System.setProperty("JDBC_DATABASE_URL", "jdbc:postgresql://" + Config.get("DB_HOST", "localhost") + ":" + Config.get("DB_PORT", "5432") + "/" + serviceName);
        System.setProperty("DATABASE_USER", Config.get("DB_USER", "devuser"));
        System.setProperty("DATABASE_USER_PWD", Config.get("DB_PASSWORD", "dev@123"));
        System.setProperty("DATABASE_POOL_SIZE", Config.get("DB_POOL_SIZE", "5"));
        System.setProperty("DATABASE_DDL_MODE", Config.get("DB_DDL_AUTO", "validate"));
        TimeZone.setDefault(TimeZone.getTimeZone(Config.get("DB_TIMEZONE", "UTC")));
    }

    public DataSource get() {
        return dataSource;
    }
}
