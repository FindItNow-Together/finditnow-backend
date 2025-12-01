package com.finditnow.userservice.config;

import com.finditnow.config.Config;

import java.util.TimeZone;

public class EnvBootstrap {
    public static void setEnv() {
        System.setProperty("SERVICE_PORT", Config.get("SERVICE_PORT", "8081"));
        System.setProperty("JDBC_DATABASE_URL", "jdbc:postgresql://" + Config.get("DB_HOST", "localhost") + ":" + Config.get("DB_PORT", "5432") + "/" + "user_service");
        System.setProperty("DATABASE_USER", Config.get("DB_USER", "devuser"));
        System.setProperty("DATABASE_USER_PWD", Config.get("DB_PASSWORD", "dev@123"));
        System.setProperty("DATABASE_POOL_SIZE", Config.get("DB_POOL_SIZE", "5"));
        TimeZone.setDefault(TimeZone.getTimeZone(Config.get("DB_TIMEZONE", "UTC")));
    }
}