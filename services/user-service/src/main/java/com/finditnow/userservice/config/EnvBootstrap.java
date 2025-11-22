package com.finditnow.userservice.config;

import com.finditnow.config.Config;

import java.util.TimeZone;

public class EnvBootstrap {
    public static void setEnv() {
        System.setProperty("SERVICE_PORT", Config.get("SERVICE_PORT", "8080"));
        System.setProperty("JDBC_DATABASE_URL", Config.get("JDBC_DATABASE_URL"));
        System.setProperty("DATABASE_USER", Config.get("DATABASE_USER"));
        System.setProperty("DATABASE_USER_PWD", Config.get("DATABASE_USER_PWD"));
        System.setProperty("DATABASE_POOL_SIZE", Config.get("DATABASE_POOL_SIZE", "5"));
        TimeZone.setDefault(TimeZone.getTimeZone(Config.get("DATABASE_TIMEZONE", "UTC")));
    }
}